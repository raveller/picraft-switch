#!/bin/bash
# PI Craft web Server stop script

# Check if server is running
if ! screen -list | grep -q "picraft-switch"; then
  echo "Server is not currently running!"
  exit 1
fi

# Stop the server
echo "Stopping PI Craft switch web server ..."
screen -Rd picraft-switch -X stuff "say Closing server (stop.sh called)...$(printf '\r')"
curl -X POST localhost:8080/actuator/shutdown

# Wait up to 30 seconds for server to close
StopChecks=0
while [ $StopChecks -lt 30 ]; do
  if ! screen -list | grep -q "picraft-switch"; then
    break
  fi
  sleep 1;
  StopChecks=$((StopChecks+1))
done

# Force quit if server is still open
if screen -list | grep -q "picraft-switch"; then
  echo "PI Craft switch web server still hasn't closed after 30 seconds, closing screen manually"
  screen -S picraft-switch -X quit
fi

echo "PI Craft switch web server stopped."

# Sync all filesystem changes out of temporary RAM
sync
