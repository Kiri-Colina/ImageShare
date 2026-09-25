from flask import Flask, request, jsonify, send_from_directory
import sqlite3
import os
import uuid
import atexit
from os.path import basename

app = Flask(__name__)
UPLOAD_FOLDER = 'uploads'
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER

if not os.path.exists(UPLOAD_FOLDER):
    os.makedirs(UPLOAD_FOLDER)


def init_db():
    conn = sqlite3.connect('users.db')
    cursor = conn.cursor()
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS users (
            id INTEGER PRIMARY KEY,
            username TEXT UNIQUE,
            password TEXT,
            display_name TEXT
        )
    ''')
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS shared_images (
            id TEXT PRIMARY KEY,
            sender_id INTEGER,
            recipient_id INTEGER,
            image_path TEXT,
            is_received INTEGER DEFAULT 0
        )
    ''')
    conn.commit()
    conn.close()


def get_db_connection():
    conn = sqlite3.connect('users.db')
    conn.row_factory = sqlite3.Row
    return conn


# 服务器关闭时清理所有未接收图片
def cleanup_pending_images():
    print("开始清理未接收图片...")

    conn = None
    try:
        conn = get_db_connection()
        cursor = conn.cursor()

        # 查询所有未接收的图片
        cursor.execute('SELECT id, image_path FROM shared_images WHERE is_received = 0')
        pending_images = cursor.fetchall()

        # 删除图片文件和数据库记录
        deleted_count = 0
        for image in pending_images:
            image_id = image['id']
            image_path = image['image_path']

            # 删除图片文件
            if os.path.exists(image_path):
                try:
                    os.remove(image_path)
                    deleted_count += 1
                    print(f"已删除未接收图片: {image_path}")
                except Exception as e:
                    print(f"删除图片失败 {image_path}: {str(e)}")

            # 删除数据库记录
            cursor.execute('DELETE FROM shared_images WHERE id =?', (image_id,))

        if deleted_count > 0:
            conn.commit()
            print(f"已清理 {deleted_count} 张未接收图片及相关数据库记录")

    except Exception as e:
        print(f"清理过程出错: {str(e)}")
    finally:
        if conn:
            conn.close()
    print("服务器关闭完成")


# 注册清理函数，服务器关闭时执行
atexit.register(cleanup_pending_images)


@app.route('/')
def test():
    return "Server is running!"


@app.route('/register', methods=['POST'])
def register():
    data = request.get_json()
    username = data.get('username')
    password = data.get('password')
    displayName = data.get('displayName')

    if not username or not password or not displayName:
        return jsonify({'success': False, 'message': 'Missing required fields'}), 400

    conn = get_db_connection()
    cursor = conn.cursor()

    try:
        cursor.execute('SELECT * FROM users WHERE username =?', (username,))
        user = cursor.fetchone()
        if user:
            return jsonify({'success': False, 'message': 'Username already exists'}), 400

        cursor.execute(
            'INSERT INTO users (username, password, display_name) VALUES (?,?,?)',
            (username, password, displayName)
        )
        conn.commit()
        return jsonify({'success': True, 'message': 'Registration successful'}), 200
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500
    finally:
        conn.close()


@app.route('/login', methods=['POST'])
def login():
    data = request.get_json()
    username = data.get('username')
    password = data.get('password')

    if not username or not password:
        return jsonify({'success': False, 'message': 'Missing required fields'}), 400

    conn = get_db_connection()
    cursor = conn.cursor()

    try:
        cursor.execute('SELECT * FROM users WHERE username =? AND password =?', (username, password))
        user = cursor.fetchone()
        if user:
            print(f"用户登录 - ID: {user['id']}, 用户名: {username}")
            return jsonify({
                'success': True,
                'user': {
                    'id': user['id'],
                    'username': user['username'],
                    'displayName': user['display_name']
                }
            }), 200
        else:
            return jsonify({'success': False, 'message': 'Invalid credentials'}), 401
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500
    finally:
        conn.close()


@app.route('/users', methods=['GET'])
def get_all_users():
    conn = get_db_connection()
    cursor = conn.cursor()

    try:
        cursor.execute('SELECT * FROM users')
        users = cursor.fetchall()
        user_list = []
        for user in users:
            user_info = {
                'id': user['id'],
                'username': user['username'],
                'displayName': user['display_name']
            }
            user_list.append(user_info)
        print(f"获取用户列表: {user_list}")
        return jsonify(user_list), 200
    except Exception as e:
        return jsonify({'error': str(e)}), 500
    finally:
        conn.close()


@app.route('/upload_image', methods=['POST'])
def upload_image():
    if 'image' not in request.files:
        return jsonify({'success': False, 'message': 'No image provided'}), 400

    image = request.files['image']
    if image.filename == '':
        return jsonify({'success': False, 'message': 'No selected image'}), 400

    recipient_id = request.form.get('recipientId')
    sender_id = request.form.get('senderId')

    print(f"图片上传 - 发送者ID: {sender_id}, 接收者ID: {recipient_id}")

    try:
        int(recipient_id)
        int(sender_id)
    except ValueError:
        return jsonify({'success': False, 'message': 'Invalid ID format (must be integer)'}), 400

    if not recipient_id or not sender_id:
        return jsonify({'success': False, 'message': 'Missing recipient or sender ID'}), 400

    image_id = str(uuid.uuid4())
    image_path = os.path.join(app.config['UPLOAD_FOLDER'], f'{image_id}.jpg')
    try:
        image.save(image_path)
    except Exception as e:
        return jsonify({'success': False, 'message': f'Failed to save image: {str(e)}'}), 500

    conn = get_db_connection()
    cursor = conn.cursor()
    try:
        cursor.execute(
            'INSERT INTO shared_images (id, sender_id, recipient_id, image_path) VALUES (?,?,?,?)',
            (image_id, sender_id, recipient_id, image_path)
        )
        conn.commit()
        print(f"数据库插入 - 图片ID: {image_id}, 接收者ID: {recipient_id}")
        return jsonify({'success': True, 'message': 'Image uploaded successfully'}), 200
    except Exception as e:
        if os.path.exists(image_path):
            os.remove(image_path)
        return jsonify({'success': False, 'message': str(e)}), 500
    finally:
        conn.close()


@app.route('/uploads/<path:filename>')
def uploaded_file(filename):
    return send_from_directory(app.config['UPLOAD_FOLDER'], filename)


@app.route('/get_pending_images', methods=['GET'])
def get_pending_images():
    user_id = request.args.get('userId')
    print(f"查询待接收图片 - 用户ID: {user_id}")

    if not user_id:
        return jsonify({'success': False, 'message': 'Missing user ID'}), 400

    try:
        int(user_id)
    except ValueError:
        return jsonify({'success': False, 'message': 'Invalid user ID format'}), 400

    conn = get_db_connection()
    cursor = conn.cursor()
    try:
        cursor.execute('SELECT * FROM shared_images WHERE recipient_id =? AND is_received = 0', (user_id,))
        images = cursor.fetchall()
        print(f"查询结果 - 找到 {len(images)} 张待接收图片")

        image_list = []
        for image in images:
            image_filename = basename(image['image_path'])
            image_list.append({
                'id': image['id'],
                'sender_id': image['sender_id'],
                'image_path': image_filename
            })
        return jsonify(image_list), 200
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500
    finally:
        conn.close()


@app.route('/mark_image_received', methods=['POST'])
def mark_image_received():
    data = request.get_json()
    image_id = data.get('imageId')
    if not image_id:
        return jsonify({'success': False, 'message': 'Missing image ID'}), 400

    conn = get_db_connection()
    cursor = conn.cursor()
    try:
        # 先获取图片路径
        cursor.execute('SELECT image_path FROM shared_images WHERE id =? AND is_received = 0', (image_id,))
        image = cursor.fetchone()

        if not image:
            return jsonify({'success': False, 'message': 'Image not found or already received'}), 404

        # 更新状态为已接收
        cursor.execute('UPDATE shared_images SET is_received = 1 WHERE id =?', (image_id,))
        conn.commit()

        # 删除图片文件
        image_path = image['image_path']
        if os.path.exists(image_path):
            os.remove(image_path)
            print(f"图片已接收并删除: {image_path}")

        return jsonify({'success': True, 'message': 'Image received and deleted successfully'}), 200
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500
    finally:
        conn.close()


if __name__ == '__main__':
    init_db()
    app.run(debug=True, host='0.0.0.0', port=5000)
