

# API
### POST: */v1/initializeTumble*
Body:
```json
{
"amount":1.0,
"fromAddress":"fromAddress",
"toAddresses":["toAddress_01", "toAddress_02", "toAddress_03"]
}
```
Returns: UUID representing the job for polling later
#
### GET: */v1/pollForCompletion*
Params: "tumbleId" -> UUID (provided by initializeTumble response)

Returns: String in the format: "TumbleID:51da3352-3802-44f3-8083-6bfb398e0667 - Percent Complete:  12"


# Progression Notes
I went from reading the documentation, to seeding the project, to completing and submitting it in 6 or 7 hours of work.
It runs in Intellij, with the Scala plugin, like any other Scala Play project. 
I used recent versions of Scala, Play, and Intellij.

## Step 1: Seeding Project 
Seeding the project didn't go as smoothly as would have been ideal. 
I started this project fresh, no template used other than the starting Intellij Scala Setup.
The default scala project in Intellij doesn't seem up to date, or it was expecting a different framework because I had to make changes.
I've been adding files such as conf/routes for API endpoints and project/plugins.sbt to get play framework set up correctly.

I was able to run the sbt project and hit my localhost to get a 200 from the health endpoint I set up.

## Step 2: Initial Endpoints
I've created 2 endpoints
1) POST the tumble request. Fires off the tumbler process asynchronously and returns a UUID that can be used to poll for status.
2) GET for polling the completion status given the ID returned by the POST.

The deposits are supposed to happen at random time intervals to confuse prying eyes. 
Therefore, we should spawn a future that can take a while to complete
and provide a way for the client to verify once it's done.

The controller has some basic error handling, 
but the meat of the tumbler hasn't been started yet.

## Step 3: Tumbler Logic 
I've created 10 house addresses
1) choose random amounts to pull from the client from address
2) put those random amounts into the house addresses until requested amounts have been withdrawn
3) wait a random amount of time before withdrawing from house addresses
4) adjust amount to withdraw from house by 2% as a fee
5) choose a random client to address provided for each of the house address to deposit into


## Step 4: Daily Skimmer Job
Create a job that runs every 24 hours that caps the house addresses.
Any amount over the cap will be deposited into the "bank" address as payment for the service.

## Step 5: Nice To Haves (If This Were In Production)
1) Code Cleanup. Some of this isn't quite as pretty as I'd like. If I had more time, I would have made it more robust.
   1) Better formatted responses and better error messages (json response DTO rather than strings)
   2) Retry logic at various stages 
2) Unit tests. I started this, but ran out of time.
3) Docker Setup. Adding docker files isn't difficult and helps with deployment immensely 
4) I'm checking that the from address exists, but not all the to addresses. I should add that as well.