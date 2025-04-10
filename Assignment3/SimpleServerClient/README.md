##### Author: Instructor team SE, ASU Polytechnic, CIDSE, SE


##### Purpose
This program shows a very simple client server implementation. The server
has 3 services, echo, add, addmany. Basic error handling on the server side
is implemented. Client does not have error handling and only has hard coded
calls to the server.

* Please run `gradle Server` and `gradle Client` together.
* Program runs on localhost
* Port is hard coded

## Protocol: ##

### Echo: ###

Request: 

    {
        "type" : "echo", -- type of request
        "data" : <String>  -- String to be echoed 
    }

General response:

    {
        "type" : "echo", -- echoes the initial response
        "ok" : <bool>, -- true or false depending on request
        "echo" : <String>,  -- echoed String if ok true
        "message" : <String>,  -- error message if ok false
    }

Success response:

    {
        "type" : "echo",
        "ok" : true,
        "echo" : <String> -- the echoed string
    }

Error response:

    {
        "type" : "echo",
        "ok" : false,
        "message" : <String> -- what went wrong
    }

### Add: ### 
Request:

    {
        "type" : "add",
        "num1" : <String>, -- first number -- String needs to be an int number e.g. "3"
        "num2" : <String> -- second number -- String needs to be an int number e.g. "4" 
    }

General response

    {
        "type" : "add", -- echoes the initial request
        "ok" : <bool>, -- true or false depending on request
        "result" : <int>,  -- result if ok true
        "message" : <String>,  -- error message if ok false
    }

Success response:

    {
        "type" : "add",
        "ok" : true,
        "result" : <int> -- the result of add
    }

Error response:

    {
        "type" : "add",
        "ok" : false,
        "message" : <String> - error message about what went wrong
    }


### AddMany: ### 
Another request, this one does not just get two numbers but an array of numbers

Request:

    {
        "type" : "addmany",
        "nums" : [<String>], -- json array of ints but given as Strings, e.g. ["1", "2"]
    }

General response:

    {
        "type"  : "addmany", -- echoes the initial request
        "ok" : <bool>, -- true or false depending on request
        "result" : <int>, -- result if 'ok' true
        "message" : <String>, -- error message if 'ok' false
    }

Success response:

    {
        "type" : "addmany",
        "ok" : true,
        "result" : <int> -- the result of adding
    }

Error response:

    {
        "type" : "addmany",
        "ok" : false, 
        "message" : <String> - error message about what went wrong
    }

### StringConcatenation: ###
This service will concatenate two strings provided by the client. The client will send a request to the server with two strings to be concatenated. The server will concatenate the strings and send back the result to the client.

Request:

    {
        "type" : "stringconcatenation",
        "string1" : <String>, -- first string
        "string2" : <String> -- second string
    }

General response:

    {
        "type" : "stringconcatenation",
        "ok" : <bool>, -- true or false depending on request
        "result" : <String>,  -- concatenated string if ok true
        "message" : <String>  -- error message if ok false
    }

Success response:

    {
        "type" : "stringconcatenation",
        "ok" : true,
        "result" : <String> -- concatenated string
    }

Error response:

    {
        "type" : "stringconcatenation",
        "ok" : false,
        "message" : <String> -- error message about what went wrong
    }


### CharCount: ###
This will count number of characters in a given string, with the option to search for a specific character in the string.
If the user specifies a character to search for, alter the request to include the character and return the number of times that character is found in the string
If the user does not specifiy a character to search for, return the number of characters in the string

Request:

    {
        "type" :  "charcount",
        "findchar" : false, -- value is false to denote general character counting
        "count" : <String> -- String to search through e.g. "sally sold seashells down by the seashore"
        "search" : <char> -- Character to count in given String
    }

Request for searching for specific char:

    {
        "type" : "charcount", 
        "findchar" : true, -- value is true to denote specific character search
        "find" : <char>, -- if findchar is true -- character in String to search for e.g. "s"
        "count" : <String> -- String to search through e.g. "sally sold seashells down by the seashore"
    }

General response:

    {
        "type" : "charcount", -- echoes the initial request
        "ok" : <bool>, -- true or false depending on request
        "result" : <int>, -- result if ok true - number of given character or overall characters in the String
        "message" : <String> -- error message if 'ok' false
    }

Success response: 

    {
        "type" : "charcount",
        "ok" : true,
        "result" : <int> -- number of the given character or overall characters in the String
    }

Error response: 

    {
        "type" : "charcount",
        "ok" : false,
        "message" : <String> -- error message about what went wrong
    }

### QuizGame: ###
This service will allow the client to play a quiz game. The server will store a set of questions and their corresponding answers. The client can choose to either add new questions or play the game. If the client chooses to add new questions, they can send a request to the server with the new question and answer. If the client chooses to play the game, the server will randomly select a question from the existing set and send it to the client. The client will respond with the answer. The server will check if the answer is correct and send the result back to the client. The game will continue until a certain number of questions have been answered or a certain time limit has been reached. The questions do not have to persist if the server shuts off, it is nice if they do but they do not have to

Request to add a new question:

    {
        "type" : "quizgame",
        "addQuestion" : true,  -- true if adding questions, false if playing game
        "question" : <String>, -- new question only if addQuestion = true
        "answer" : <String>    -- answer to the new question only if addQuestion = true
    }

Success response:

    {
        "type" : "quizgame",
        "ok" : true
    }

Error response:

    {
        "type" : "quizgame",
        "ok" : false,
        "message" : <String> -- error message about what went wrong
    }


Request to play the game:

    {
        "type" : "quizgame",
        "addQuestion" : false
    }

Success response:

    {
        "type" : "quizgame",
        "ok" : true,
        "question" : <String>,  -- question to be answered
    }

Error response:

    {
        "type" : "quizgame",
        "ok" : false,
        "message" : <String> -- error message about what went wrong
    }


Request to answer a question:

    {
        "type" : "quizgame",
        "answer" : <String> -- client's answer to the question
    }

Success response:

    {
        "type" : "quizgame",
        "ok" : true,
        "question" : <String>, -- question only if result is false
        "result" : <bool> -- result of the answer (true if correct, false if incorrect)
    }

Error response:

    {
        "type" : "quizgame",
        "ok" : false,
        "message" : <String> -- error message about what went wrong
    }


### General error responses: ###
These are used for all requests.

Error response: When a required field "key" is not in request

    {
        "ok" : false
        "message" : "Field <key> does not exist in request" 
    }

Error response: When a required field "key" is not of correct "type"

    {
        "ok" : false
        "message" : "Field <key> needs to be of type: <type>"
    }

Error response: When the "type" is not supported, so an unsupported request

    {
        "ok" : false
        "message" : "Type <type> is not supported."
    }


Error response: When the "type" is not supported, so an unsupported request

    {
        "ok" : false
        "message" : "req not JSON"
    }