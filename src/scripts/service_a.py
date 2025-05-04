from flask import Flask, jsonify, request

app = Flask(__name__)

@app.route('/users', methods=['GET'])
def get_users():
    return jsonify({
        "users": [
            {"id": 1, "name": "John Doe", "email": "john@example.com"},
            {"id": 2, "name": "Jane Smith", "email": "jane@example.com"},
            {"id": 3, "name": "Bob Johnson", "email": "bob@example.com"}
        ],
        "service": "user-service",
        "instance": "instance-1"
    })

@app.route('/<path:subpath>', methods=['GET', 'POST', 'PUT', 'DELETE'])
def catch_all(subpath):
    print(f"User service received request for: /{subpath}")
    return jsonify({
        "message": f"Response from user-service for path: /{subpath}",
        "service": "user-service",
        "instance": "instance-1",
        "method": request.method
    })

if __name__ == '__main__':
    print("Starting user service on port 8081...")
    app.run(host='0.0.0.0', port=8081, debug=True)