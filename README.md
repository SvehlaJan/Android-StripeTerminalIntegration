# Sample Android Stripe SDK integration

This is a sample Android app that demonstrates how to use the Stripe Android SDK to accept payments on Stripe Terminals.

## About this app

Was built on top of sample implementations from Stripe Docs - https://stripe.com/docs/terminal/quickstart?platform=android&lang=python

App is written in Kotlin, inspired with Android Architecture guidelines and Clean Architecture.

UI is done with Jetpack Compose.

Parts of Stripe Android SDK are wrapped in coroutines and Flows.

A few unit tests are included.

## How to run

Android app should compile without any additional steps.

To test the app, a backend server is required. I used a Docker container with https://github.com/stripe/example-terminal-backend .

Set the backend URL in `tech.svehla.demo.data.ApiClient` and run the app.