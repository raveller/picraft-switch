#!/bin/bash
export PICRAFT_VANILLA_WORLDS_PATH=/home/pi/minecraft-vanilla
export PICRAFT_VANILLA_START_COMMAND=minecraft.sh
export PICRAFT_VANILLA_TEMPLATE=/home/pi/minecraft-vanilla/template
export PICRAFT_PAPER_WORLDS_PATH=/home/pi/minecraft-paper
export PICRAFT_PAPER_START_COMMAND=start.sh
export PICRAFT_PAPER_TEMPLATE=/home/pi/minecraft-paper/template
export PICRAFT_STOP_COMMAND=/home/pi/stop-minecraft.sh


screen -dmS picraft-switch java -jar picraft-switch-0.0.1-SNAPSHOT.jar -Xmx100m


