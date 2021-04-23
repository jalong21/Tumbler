# Progression Notes

##Step 1: Seeding Project
Seeding project didn't go as smoothly as would be ideal.
The default scala project in Intellij doesn't seem up to date.
I've been adding files such as conf/routes for API endpoints and project/plugins.sbt to get play framework.

I was able to run the sbt project and hit my localhost to get a 200 from the health endpoint I set up

##Step 2: Initial Endpoints
I've created 2 endpoints
1) for submitting the tumble request. Returns an ID for the request.
2) for polling for the completion status.

The idea being, since this is supposed to happen at random intervals, 
in order to confuse prying eyes. 
Therefore, we should spawn a future that can take a while to complete
and provide a way for the client to verify once it's done.

The controller has some basic error handling, 
but the meat of the tumbler hasn't been started yet.