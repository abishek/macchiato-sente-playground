# Example Macchiato Project With Sente Websockets

This project explores adding sente websockets to a macchiato application.

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

This will launch the backend servers. Browse to `http://localhost:3000` to launch the landing page that will also try to connect to the websockets. 
