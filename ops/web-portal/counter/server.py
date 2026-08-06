#!/usr/bin/env python3
import json
import os
import sqlite3
from http.server import BaseHTTPRequestHandler, HTTPServer
from pathlib import Path
from socketserver import ThreadingMixIn
from urllib.parse import quote, unquote, urlsplit


DATABASE = Path(os.environ.get("DOWNLOAD_DATABASE", "/data/downloads.db"))
PORT = int(os.environ.get("DOWNLOAD_COUNTER_PORT", "8080"))
FILES = {
    "Eagle-PBX-Mobile-0.1.63-debug.apk",
    "Eagle-PBX-Mobile-0.1.62-debug.apk",
    "Eagle-PBX-Mobile-0.1.61-debug.apk",
    "Eagle-PBX-Mobile-0.1.60-debug.apk",
    "Eagle-PBX-Mobile-0.1.59-debug.apk",
    "Eagle-PBX-Mobile-0.1.58-debug.apk",
    "Eagle-PBX-Mobile-0.1.57-debug.apk",
    "Eagle-PBX-Mobile-0.1.56-debug.apk",
    "Eagle-PBX-Mobile-0.1.55-debug.apk",
    "Eagle-PBX-Mobile-0.1.54-debug.apk",
    "Eagle-PBX-Mobile-0.1.53-debug.apk",
    "Eagle-PBX-Mobile-0.1.52-debug.apk",
    "Eagle-PBX-Mobile-0.1.51-debug.apk",
    "Eagle-PBX-Mobile-0.1.50-debug.apk",
    "Eagle-PBX-Mobile-0.1.49-debug.apk",
    "Eagle-PBX-Mobile-0.1.48-debug.apk",
    "Eagle-PBX-Mobile-0.1.47-debug.apk",
    "Eagle-PBX-Mobile-0.1.46-debug.apk",
    "Eagle-PBX-Mobile-0.1.45-debug.apk",
    "Eagle-PBX-Mobile-0.1.44-debug.apk",
    "Eagle-PBX-Mobile-0.1.43-debug.apk",
    "Eagle-PBX-Mobile-0.1.42-debug.apk",
    "Eagle-PBX-Mobile-0.1.41-debug.apk",
    "Eagle-PBX-Mobile-0.1.40-debug.apk",
    "Eagle-PBX-Mobile-0.1.39-homologacao.apk",
    "Eagle-PBX-Mobile-0.1.38-homologacao.apk",
}


def connect():
    connection = sqlite3.connect(str(DATABASE), timeout=10)
    connection.execute(
        "CREATE TABLE IF NOT EXISTS downloads (filename TEXT PRIMARY KEY, total INTEGER NOT NULL DEFAULT 0)"
    )
    connection.executemany(
        "INSERT OR IGNORE INTO downloads (filename, total) VALUES (?, 0)",
        ((filename,) for filename in FILES),
    )
    connection.commit()
    return connection


def counts():
    with connect() as connection:
        rows = connection.execute("SELECT filename, total FROM downloads").fetchall()
    return {filename: total for filename, total in rows if filename in FILES}


def register(filename):
    with connect() as connection:
        connection.execute(
            "UPDATE downloads SET total = total + 1 WHERE filename = ?", (filename,)
        )
        connection.commit()


class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        path = urlsplit(self.path).path
        if path == "/health":
            return self.send_json({"status": "ok"})
        if path == "/counts":
            return self.send_json(counts())
        if path.startswith("/download/"):
            filename = unquote(path[len("/download/"):])
            if filename not in FILES:
                return self.send_error(404)
            register(filename)
            self.send_response(302)
            self.send_header("Location", f"/pbx/apk/download/{quote(filename)}")
            self.send_header("Cache-Control", "no-store")
            self.send_header("Content-Length", "0")
            self.end_headers()
            return
        self.send_error(404)

    def send_json(self, payload):
        body = json.dumps(payload, ensure_ascii=False, separators=(",", ":")).encode()
        self.send_response(200)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Cache-Control", "no-store")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, _format, *_args):
        pass


class ThreadingHTTPServer(ThreadingMixIn, HTTPServer):
    daemon_threads = True


if __name__ == "__main__":
    DATABASE.parent.mkdir(parents=True, exist_ok=True)
    with connect():
        pass
    ThreadingHTTPServer(("0.0.0.0", PORT), Handler).serve_forever()
