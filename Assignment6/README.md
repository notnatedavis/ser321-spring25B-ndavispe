# Description (w/ Requirements filled)
This is a client server application where the server implements two additional services from the .protos that were given including an additional unique service.

# Execution Operation
`gradle runNode -PregistryHost="localhost" -PgrpcPort=9000 -PserviceHost="localhost" -PservicePort=8000 -PnodeName="FitnessZodiacLibraryNode" -PregOn=false`  

`gradle runClient -Phost="localhost" -Pport=8000 -PregHost="localhost" -PregPort=9000 -Pmessage="test" -PregOn=false`  

# Work Flow
launch Node, launch Client (connects to the node), select service to use, follow CLI prompts.

# Requirements
- Run service node through `gradle runNode` which uses default args
- Run client through `gradle runClient` using correct default args to connect to started service node
- Implement 2 services (Fitness & Zodiac)
- Create unique service (Library) {data persistent}
- Calls and user interaction are easy
- Server and Client are robust and do not crash

# Screencast
(https://youtu.be/Z-5iekshKPc)