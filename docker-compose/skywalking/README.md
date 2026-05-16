# SkyWalking Agent Setup

1. Download the agent:
   wget https://dlcdn.apache.org/skywalking/java-agent/9.3.0/apache-skywalking-java-agent-9.3.0.tgz
   tar -xzf apache-skywalking-java-agent-9.3.0.tgz -C ./agent/

2. Start a service with the agent:
   JAVA_TOOL_OPTIONS="-javaagent:./docker-compose/skywalking/agent/skywalking-agent.jar -DSW_AGENT_NAME=service-user -DSW_AGENT_COLLECTOR_BACKEND_SERVICES=localhost:11800" \
     mvn -pl super-market-services/service-user spring-boot:run

3. Open SkyWalking UI at http://localhost:18083
