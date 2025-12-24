#!/usr/bin/env python3
"""
Сервер "Умный список покупок" для Raspberry Pi
Только: название товара, статус покупки, заметки
"""

import sqlite3
import os
from datetime import datetime
from flask import Flask, request, jsonify
from flask_cors import CORS

# ==================== НАСТРОЙКИ ====================
app = Flask(__name__)
CORS(app)  # Разрешаем CORS для Android

DATABASE = 'shopping.db'
HOST = '0.0.0.0'  # Принимаем подключения со всех интерфейсов
PORT = 5000

# ==================== БАЗА ДАННЫХ ====================
def get_db_connection():
    """Создать соединение с базой данных"""
    conn = sqlite3.connect(DATABASE)
    conn.row_factory = sqlite3.Row  # Возвращать строки как словари
    return conn

def init_database():
    """Создать таблицы если их нет"""
    conn = get_db_connection()
    cursor = conn.cursor()
    
    # Таблица товаров (только 3 поля)
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS products (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,           -- Название товара
            purchased BOOLEAN DEFAULT 0,  -- Куплен ли (0/1)
            notes TEXT DEFAULT '',        -- Заметки к товару
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    ''')
    
    # Добавляем тестовые данные если таблица пуста
    cursor.execute("SELECT COUNT(*) FROM products")
    if cursor.fetchone()[0] == 0:
        test_products = [
            ('Молоко', 0, 'Взять 2 пакета'),
            ('Хлеб', 1, 'Черный, бородинский'),
            ('Яйца', 0, '10 штук, категория С0'),
            ('Кофе', 0, 'Молотый, Arabica'),
            ('Сахар', 0, '1 кг, коричневый')
        ]
        
        for name, purchased, notes in test_products:
            cursor.execute(
                "INSERT INTO products (name, purchased, notes) VALUES (?, ?, ?)",
                (name, purchased, notes)
            )
        
        print(f"✅ Добавлено {len(test_products)} тестовых товаров")
    
    conn.commit()
    conn.close()
    print(f"✅ База данных создана: {DATABASE}")

# ==================== ВАЛИДАЦИЯ ====================
def validate_product(data):
    """Проверка данных товара"""
    errors = []
    
    # Проверка названия
    name = data.get('name', '').strip()
    if not name:
        errors.append("Название товара обязательно")
    elif len(name) > 100:
        errors.append("Название слишком длинное (макс 100 символов)")
    
    # Проверка статуса покупки
    purchased = data.get('purchased')
    if purchased is not None and not isinstance(purchased, bool):
        errors.append("Поле 'purchased' должно быть true/false")
    
    # Проверка заметок
    notes = data.get('notes', '')
    if notes and len(notes) > 500:
        errors.append("Заметки слишком длинные (макс 500 символов)")
    
    return errors

# ==================== API ENDPOINTS ====================

@app.route('/api/products', methods=['GET'])
def get_all_products():
    """Получить все товары"""
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        
        # Параметры фильтрации
        purchased_filter = request.args.get('purchased')
        
        # Базовый запрос
        query = "SELECT id, name, purchased, notes, created_at FROM products"
        params = []
        
        if purchased_filter is not None:
            query += " WHERE purchased = ?"
            params.append(1 if purchased_filter.lower() == 'true' else 0)
        
        query += " ORDER BY purchased, created_at DESC"
        
        cursor.execute(query, params)
        rows = cursor.fetchall()
        conn.close()
        
        # Преобразуем в JSON
        products = []
        for row in rows:
            products.append({
                'id': row['id'],
                'name': row['name'],
                'purchased': bool(row['purchased']),
                'notes': row['notes'],
                'created_at': row['created_at']
            })
        
        return jsonify({
            'success': True,
            'count': len(products),
            'data': products,
            'timestamp': datetime.now().isoformat()
        })
        
    except Exception as e:
        print(f"❌ Ошибка получения товаров: {e}")
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

@app.route('/api/products/<int:product_id>', methods=['GET'])
def get_product(product_id):
    """Получить один товар по ID"""
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        
        cursor.execute(
            "SELECT id, name, purchased, notes, created_at FROM products WHERE id = ?",
            (product_id,)
        )
        row = cursor.fetchone()
        conn.close()
        
        if not row:
            return jsonify({
                'success': False,
                'error': 'Товар не найден'
            }), 404
        
        return jsonify({
            'success': True,
            'data': {
                'id': row['id'],
                'name': row['name'],
                'purchased': bool(row['purchased']),
                'notes': row['notes'],
                'created_at': row['created_at']
            }
        })
        
    except Exception as e:
        print(f"❌ Ошибка получения товара {product_id}: {e}")
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

@app.route('/api/products', methods=['POST'])
def create_product():
    """Создать новый товар"""
    try:
        data = request.get_json()
        
        # Валидация
        errors = validate_product(data)
        if errors:
            return jsonify({
                'success': False,
                'errors': errors
            }), 400
        
        # Извлекаем данные (только 3 поля!)
        name = data.get('name', '').strip()
        purchased = data.get('purchased', False)
        notes = data.get('notes', '').strip()
        
        # Сохраняем в базу
        conn = get_db_connection()
        cursor = conn.cursor()
        
        cursor.execute(
            "INSERT INTO products (name, purchased, notes) VALUES (?, ?, ?)",
            (name, 1 if purchased else 0, notes)
        )
        
        product_id = cursor.lastrowid
        conn.commit()
        
        # Получаем созданный товар
        cursor.execute(
            "SELECT id, name, purchased, notes FROM products WHERE id = ?",
            (product_id,)
        )
        product = cursor.fetchone()
        conn.close()
        
        print(f"✅ Создан товар: {name} (ID: {product_id})")
        
        return jsonify({
            'success': True,
            'message': 'Товар успешно создан',
            'data': {
                'id': product['id'],
                'name': product['name'],
                'purchased': bool(product['purchased']),
                'notes': product['notes']
            }
        }), 201  # 201 Created
        
    except Exception as e:
        print(f"❌ Ошибка создания товара: {e}")
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

@app.route('/api/products/<int:product_id>', methods=['PUT'])
def update_product(product_id):
    """Обновить товар"""
    try:
        data = request.get_json()
        
        # Проверяем что есть что обновлять
        if not data:
            return jsonify({
                'success': False,
                'error': 'Нет данных для обновления'
            }), 400
        
        # Валидация
        errors = validate_product(data)
        if errors:
            return jsonify({
                'success': False,
                'errors': errors
            }), 400
        
        conn = get_db_connection()
        cursor = conn.cursor()
        
        # Проверяем существует ли товар
        cursor.execute("SELECT id FROM products WHERE id = ?", (product_id,))
        if not cursor.fetchone():
            conn.close()
            return jsonify({
                'success': False,
                'error': 'Товар не найден'
            }), 404
        
        # Подготавливаем данные для обновления
        updates = []
        params = []
        
        # Только 3 поля которые мы обновляем:
        if 'name' in data:
            updates.append("name = ?")
            params.append(data['name'].strip())
        
        if 'purchased' in data:
            updates.append("purchased = ?")
            params.append(1 if data['purchased'] else 0)
        
        if 'notes' in data:
            updates.append("notes = ?")
            params.append(data['notes'].strip())
        
        # Добавляем время обновления
        updates.append("updated_at = CURRENT_TIMESTAMP")
        
        # Формируем и выполняем запрос
        params.append(product_id)  # WHERE id = ?
        
        query = f"UPDATE products SET {', '.join(updates)} WHERE id = ?"
        cursor.execute(query, params)
        conn.commit()
        conn.close()
        
        print(f"✅ Обновлен товар ID: {product_id}")
        
        return jsonify({
            'success': True,
            'message': 'Товар успешно обновлен'
        })
        
    except Exception as e:
        print(f"❌ Ошибка обновления товара {product_id}: {e}")
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

@app.route('/api/products/<int:product_id>', methods=['DELETE'])
def delete_product(product_id):
    """Удалить товар"""
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        
        # Проверяем существует ли товар
        cursor.execute("SELECT name FROM products WHERE id = ?", (product_id,))
        product = cursor.fetchone()
        
        if not product:
            conn.close()
            return jsonify({
                'success': False,
                'error': 'Товар не найден'
            }), 404
        
        # Удаляем
        cursor.execute("DELETE FROM products WHERE id = ?", (product_id,))
        conn.commit()
        conn.close()
        
        print(f"✅ Удален товар: {product['name']} (ID: {product_id})")
        
        return jsonify({
            'success': True,
            'message': 'Товар успешно удален'
        })
        
    except Exception as e:
        print(f"❌ Ошибка удаления товара {product_id}: {e}")
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

# ==================== СИНХРОНИЗАЦИЯ ====================

@app.route('/api/sync', methods=['POST'])
def sync_products():
    """Синхронизация товаров (для нескольких устройств)"""
    try:
        data = request.get_json()
        
        # Получаем последние изменения с сервера
        conn = get_db_connection()
        cursor = conn.cursor()
        
        # Получаем все товары
        cursor.execute("SELECT id, name, purchased, notes, updated_at FROM products")
        server_products = cursor.fetchall()
        
        # Если клиент отправил свои изменения - обрабатываем их
        client_changes = data.get('changes', [])
        applied_changes = []
        
        for change in client_changes:
            try:
                action = change.get('action')
                
                if action == 'create':
                    cursor.execute(
                        "INSERT INTO products (name, purchased, notes) VALUES (?, ?, ?)",
                        (change['name'], change.get('purchased', 0), change.get('notes', ''))
                    )
                    
                elif action == 'update':
                    cursor.execute(
                        "UPDATE products SET name = ?, purchased = ?, notes = ? WHERE id = ?",
                        (change['name'], change.get('purchased', 0), change.get('notes', ''), change['id'])
                    )
                    
                elif action == 'delete':
                    cursor.execute("DELETE FROM products WHERE id = ?", (change['id'],))
                
                applied_changes.append(action)
                
            except Exception as e:
                print(f"⚠️ Ошибка применения изменения: {e}")
        
        conn.commit()
        
        # Формируем ответ
        products_list = []
        for row in server_products:
            products_list.append({
                'id': row['id'],
                'name': row['name'],
                'purchased': bool(row['purchased']),
                'notes': row['notes'],
                'updated_at': row['updated_at']
            })
        
        conn.close()
        
        print(f"✅ Синхронизация: отправлено {len(products_list)} товаров")
        
        return jsonify({
            'success': True,
            'message': 'Синхронизация успешна',
            'data': products_list,
            'applied_changes': applied_changes,
            'timestamp': datetime.now().isoformat()
        })
        
    except Exception as e:
        print(f"❌ Ошибка синхронизации: {e}")
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

# ==================== СИСТЕМНЫЕ ENDPOINTS ====================

@app.route('/api/status', methods=['GET'])
def get_status():
    """Получить статус сервера и статистику"""
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        
        # Статистика
        cursor.execute("SELECT COUNT(*) FROM products")
        total = cursor.fetchone()[0]
        
        cursor.execute("SELECT COUNT(*) FROM products WHERE purchased = 1")
        purchased = cursor.fetchone()[0]
        
        conn.close()
        
        return jsonify({
            'success': True,
            'data': {
                'server': 'Raspberry Pi Shopping List',
                'status': 'running',
                'total_products': total,
                'purchased_products': purchased,
                'not_purchased': total - purchased,
                'timestamp': datetime.now().isoformat()
            }
        })
        
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

@app.route('/health', methods=['GET'])
def health_check():
    """Простая проверка здоровья сервера"""
    return jsonify({
        'status': 'healthy',
        'server': 'Raspberry Pi',
        'endpoints': [
            'GET /api/products - получить все товары',
            'POST /api/products - создать товар',
            'PUT /api/products/{id} - обновить товар',
            'DELETE /api/products/{id} - удалить товар',
            'POST /api/sync - синхронизация',
            'GET /api/status - статус сервера',
            'GET /health - проверка здоровья'
        ],
        'fields': {
            'name': 'Название товара (обязательно)',
            'purchased': 'Куплен ли (true/false)',
            'notes': 'Заметки к товару (необязательно)'
        }
    })

# ==================== ЗАПУСК СЕРВЕРА ====================

def print_welcome():
    """Красивое приветственное сообщение"""
    import socket
    
    print("=" * 60)
    print("🛒 УМНЫЙ СПИСОК ПОКУПОК - СЕРВЕР НА RASPBERRY PI")
    print("=" * 60)
    print()
    print("📦 ПОЛЯ ТОВАРОВ:")
    print("  • Название товара (обязательно)")
    print("  • Статус покупки (чекбокс)")
    print("  • Заметки к товару")
    print()
    print("🌐 API ДОСТУПНО ПО АДРЕСАМ:")
    print(f"  • http://localhost:{PORT}")
    
    # Пробуем получить IP адреса
    try:
        hostname = socket.gethostname()
        ip_address = socket.gethostbyname(hostname)
        print(f"  • http://{ip_address}:{PORT}")
        
        # Для телефонов в той же сети
        print(f"  • Для Android: http://{ip_address}:{PORT}/api/products")
        
    except:
        print("  • IP адрес будет доступен после подключения к сети")
    
    print()
    print("🚀 ЗАПУСК СЕРВЕРА...")
    print("=" * 60)

if __name__ == '__main__':
    # Инициализация базы данных
    init_database()
    
    # Приветственное сообщение
    print_welcome()
    
    # Запуск сервера
    print(f"✅ Сервер запущен на порту {PORT}")
    print(f"✅ База данных: {DATABASE}")
    print(f"✅ Режим: {'разработки' if app.debug else 'продакшн'}")
    print()
    print("📝 Пример curl команды для тестирования:")
    print(f'  curl http://localhost:{PORT}/api/products')
    print()
    print("🔄 Для остановки сервера нажмите Ctrl+C")
    print("=" * 60)
    
    app.run(host=HOST, port=PORT, debug=False)