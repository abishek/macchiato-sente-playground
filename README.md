# Example Macchiato Project With Sente Websockets

Macchiato doesn't play very well with Sente (or maybe its the other way). This project is a playground to explore a quick patch between the two libraries.

## Requirements

This project uses nodejs v18. 
## Running The Example

### Start the shadow-cljs compilers

```
npm run build
```

Then run the actual server using node

```
node target/main.js
```

This will launch the backend servers. Browse to `http://localhost:3000` to launch the landing page that will also try to connect to the websockets. Follow the instructions on the page to explore the socket behaviour.

The `REPL server` becomes available when the `node` command is running. But there doesn't seem to be any support for `REPL driven development` in macchiato at this time.