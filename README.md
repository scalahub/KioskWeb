# KioskWeb
[![Build Status](https://travis-ci.com/scalahub/KioskWeb.svg?branch=main)](https://travis-ci.com/scalahub/KioskWeb)

A lightweight web-frontend wrapper over Kiosk

## Pre-reqisites

- SBT 1.0
- JDK 8

## Instructions 

First clone the project and open the folder in a terminal.

There are two ways to run the code

1. Using embedded web server (Jetty)

   Open SBT shell by typing `sbt`.

   Inside the shell issue the command `jetty:start`. 
   
   The webserver will run on `http://localhost:8080`.
   
2. Using external web server such as Tomcat
   
   Issue the command `sbt package`.
   
   This will generate a WAR file in `target/scala-2.12` folder.
   
   Use the above WAR file in your own web server
