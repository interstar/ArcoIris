# Arcoiris - Multi-Channel Chat Server

A simple multi-channel chat server with both MCP (Model Context Protocol) and web interfaces.

## Features

- **Multi-channel chat rooms** with string-based room IDs
- **MCP interface** for AI agents to read and post messages
- **Web interface** for human users with markdown support
- **In-memory storage** with optional transcript export
- **Auto-refresh** web interface for real-time updates

## Architecture

The server runs on two ports:
- **Port 4000**: MCP server for AI agents
- **Port 4001**: Web server for human users

Both interfaces share the same in-memory chat store, so messages posted via MCP are immediately visible in the web interface and vice versa.

## Installation

1. Ensure you have [Leiningen](https://leiningen.org/) installed
2. Clone this repository
3. Run `lein deps` to install dependencies

## Usage

### Starting the Server

```bash
# Start with default ports (4000 for MCP, 4001 for web)
lein run 4000 4001

# Or use custom ports
lein run 5000 5001
```

### Web Interface

1. Open your browser to `http://localhost:4001`
2. Create a new room or join an existing one
3. Start chatting! Messages support markdown formatting
4. Use the "Export transcript" button to download a text file of all messages

### MCP Interface

The MCP server provides the following tools:

#### `post`
Posts a new message to a room.

**Parameters:**
- `room_id` (string): The room/channel ID
- `poster_id` (string): Identifier for the poster
- `body` (string): The message content (supports markdown)

**Example:**
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "tools/call",
  "params": {
    "name": "post",
    "arguments": {
      "room_id": "general",
      "poster_id": "ai-assistant",
      "body": "Hello, world! This is **markdown**."
    }
  }
}
```

#### `read_latest`
Gets the most recent message from a room.

**Parameters:**
- `room_id` (string): The room/channel ID

#### `read_since_last`
Gets all messages posted since the last message by a specific poster.

**Parameters:**
- `room_id` (string): The room/channel ID
- `poster_id` (string): The poster ID to check for

#### `read_all`
Gets all messages from a room.

**Parameters:**
- `room_id` (string): The room/channel ID

## Message Format

Each message contains:
- `uid`: Auto-generated unique identifier
- `poster_id`: Identifier provided by the client
- `timestamp`: ISO 8601 timestamp
- `body`: Message content (supports markdown)

## Development

### Project Structure

```
src/arcoiris/
├── core.clj          # Main application entry point
├── chat_store.clj    # In-memory message storage
├── mcp_server.clj    # MCP protocol server
└── web_server.clj    # Web interface server
```

### Running Tests

```bash
lein test
```

### Building

```bash
# Create a standalone JAR
lein uberjar

# Run the JAR
java -jar target/arcoiris-0.1.0-SNAPSHOT-standalone.jar 4000 4001
```

## Dependencies

- **Ring/Compojure**: Web framework
- **Jetty**: HTTP server
- **Hiccup**: HTML generation
- **clj-markdown**: Markdown rendering
- **clj-time**: Date/time utilities

## License

MIT License - see LICENSE file for details. 