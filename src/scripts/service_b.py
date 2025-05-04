from flask import Flask, jsonify

app = Flask(__name__)

@app.route('/<path:subpath>', methods=['GET', 'POST', 'PUT', 'DELETE'])
def catch_all(subpath):
    print(f"Service B received request for: /{subpath}")
    return jsonify({
        "message": f"Response from Service B for path: /{subpath}",
        "service": "service-b",
        "port": 9002
    })

if __name__ == '__main__':
    app.run(port=9002, debug=True)