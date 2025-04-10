# Movie Guesser Game

## a) Project Description
a multiplayer client server game where players guess the movie titles from predetermined images which initially start pixelated and become less and less pixelated to make easier to guess with points matching level of clarity.  
**Features**:
- choose game duration (30/60/90 seconds)
- earn points based on clarity & speed or response
- limited amount of skips to a game
- live leaderboard
- client side GUI image display with buttons and actions

## b) Requirements Checklist
[x] - Start game with duration selection  
[x] - Robust handling of user input  
[x] - Handle guess/skip/next/remaining respectively   
[x] - Validate guesses and properly manage actions  
[x] - Implement skip limits (2/4/6)   
[x] - 'Next' reveals a clearer image  
[x] - 'Remaining' shows skips left  
[x] - Timer for respective duration displayed on client side  

## c) Protocol

### Join ###

Request:

    {
        "type": "join",
        "username": "<String>"
    }

General Response:

    {
        "type": "state",
        "ok": <bool>,
        "game_state": "<String>",
        "message": "<String>",
        "available_actions": ["<String>"]
    }

Success Response:

    {
        "type": "state",
        "ok": true,
        "game_state": "lobby",
        "message": "Welcome Alice! You are in the lobby",
        "available_actions": ["start", "leaderboard", "quit"]
    }

Error Response:

    {
        "type": "error",
        "ok": false,
        "message": "Missing username"
    }

### Start ###

Request:

    {
        "type": "start",
        "username": "<String>",
        "duration": "<String>"  // "short", "medium", "long"
    }

General Response:

    {
        "type": "state",
        "ok": <bool>,
        "game_state": "<String>",
        "image_data": "<Base64String>",
        "attempts": <int>,
        "current_points": <int>,
        "time_remaining": <int>,
        "available_actions": ["<String>"]
    }

Success Response:

    {
        "type": "state",
        "ok": true,
        "game_state": "running",
        "image_data": "iVBORw0KG...",
        "attempts": 3,
        "current_points": 5,
        "time_remaining": 90,
        "available_actions": ["guess", "next", "skip", "remaining", "quit"]
    }

Error Response:

    {
        "type": "error",
        "ok": false,
        "message": "Invalid duration format"
    }

### Guess ###

Request:

    {
        "type": "guess",
        "title": "<String>"
    }

General Response:

    {
        "type": "result",
        "ok": <bool>,
        "result_details": "<String>",
        "game_state": "<String>",
        "attempts_left": <int>,
        "available_actions": ["<String>"]
    }

Success Response:

    {
        "type": "result",
        "ok": true,
        "result_details": "Correct! The movie was: jaws",
        "game_state": "won",
        "available_actions": ["start", "leaderboard", "quit"]
    }

    {
        "type": "result",
        "ok": true,
        "result_details": "Incorrect guess. Try again!",
        "attempts_left": 2,
        "available_actions": ["guess", "next", "skip", "remaining", "quit"]
    }

    {
        "type": "result",
        "ok": true,
        "result_details": "Game over! Out of attempts",
        "game_state": "lost",
        "available_actions": ["start", "leaderboard", "quit"]
    }

### Next ###

Request:

    {
        "type": "next"
    }

General Response:

    {
        "type": "state",
        "ok": <bool>,
        "current_points": <int>,
        "image_data": "<Base64String>",
        "message": "<String>",
        "available_actions": ["<String>"]
    }

Success Response:

    {
        "type": "state",
        "ok": true,
        "current_points": 4,
        "image_data": "iVBORw0KG...",
        "message": "Guess the movie title. Current value: 4 points",
        "available_actions": ["guess", "next", "skip", "remaining", "quit"]
    }

Error Response:

    {
        "type": "error",
        "ok": false,
        "message": "Already at clearest image"
    }

### Skip ###

Request:

    {
        "type": "skip"
    }

General Response: 

    {
        "type": "state",
        "ok": <bool>,
        "image_data": "<Base64String>",
        "available_actions": ["<String>"]
    }

Success Response:

    {
        "type": "state",
        "ok": true,
        "image_data": "iVBORw0KG...",
        "available_actions": ["guess", "next", "skip", "remaining", "quit"]
    }

Error Response:

    {
        "type": "error",
        "ok": false,
        "message": "No skips remaining"
    }

### Remaining ###

Request:

    {
        "type": "remaining"
    }

General Response:

    { 
        "type": "remaining",
        "ok": <bool>,
        "message": "<String>",
        "time_remaining": <int>,
        "available_actions": ["<String>"]
    }

Success Response:

    {
        "type": "remaining",
        "ok": true,
        "message": "Skips remaining: 4",
        "time_remaining": 45,
        "available_actions": ["guess", "next", "skip", "remaining", "quit"]
    }

### Leaderboard ###

Request:

    {
        "type": "leaderboard"
    }

General Response:

    {
        "type": "leaderboard",
        "ok": <bool>,
        "top_players": [{"username": "<String>", "score": <int>}],
        "available_actions": ["<String>"]
    }

Success Response:

    {
        "type": "leaderboard",
        "ok": true,
        "top_players": [
            {"username": "Bob", "score": 42},
            {"username": "Alice", "score": 35}
        ],
        "available_actions": ["start", "quit"]
    }

### Quit ###

Request:

    {
        "type": "quit"
    }

General Response:

    {
        "type": "state",
        "ok": <bool>,
        "game_state": "<String>",
        "message": "<String>"
    }

Success Response:

    {
        "type": "state",
        "ok": true,
        "game_state": "inactive",
        "message": "You have left the game"
    }

### State ###

Request:

    {
        "type": "state"
    }

General Response:

    {
        "type": "state",
        "ok": <bool>,
        "game_state": "<String>",
        "time_remaining": <int>,
        "image_data": "<Base64String>",
        "available_actions": ["<String>"]
    }

## d) Link
(https://youtu.be/SfAdsdigijM)

## e) Robust
The program is designed robustly through network reliability, input validation, concurrency and error handling in mind. Network reliability is robust through an auto-reconnect upon disconnection from the client and server. Input validation is robust through both sanitizing user input and handling mal requests. Concurrency is robust through thread per client on the server, a synchronized live leaderboard, and atomic game state updates. Error handling is robust through try-catch blocks for any and all input/outputs along with clear error messages to the client.

## f) UDP Change
In the instance that TCP were to be swapped for a UDP protocol, then it would be required to change/update how messages are sent and recieved, how data is handled, how sessions are handled, and how to sync data across programs.

