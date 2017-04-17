# NewsBlur Android App

## Objective
Demonstrate the Android application model components using open sourced NewsBlur app.

## Live Site
https://newsblur.com/

## Summary
- NewsBlur app is a open sourced app
- Available in Google play store
### Capabilities:
- User sign up, login
- Search feeds, Add and delete RSS, Atom news feeds
- Multiple category views : All stories, saved stories etc
- Read news feed one by one
- Background news feed syncing
- Offline news reading from local storage (DB)
- Share news, mark as read or unread

## Modification:
Modified code from NewsBlur app and to demonstrate the below Android application model components.

## Application Components
- There are four different types of components available
- Each component is a different point through which the system can enter into the app

## Activity
- Visual representation of a screen
- Every activity will always have a User Interface
- Usually, there will  be more than one activity in an app
- Launcher or main activity is the entry point
- Activities may use below two components
  - Views -  XML Layout file that contains UI elements like Buttons, Text, Menu 
  - Fragments - Reusable components which run in the context of an activity
  
 
## Activity States
### Active or Running 
- Activity appear in the foreground
- Gets highest priority
- Get killed only on extreme case like app becomes unresponsive

### Paused 
- Activity covered partially or completely
- Gets second highest priority
- Get killed only to keep the Running activity responsive
- Stopped
- Completely hidden or in the background
- Gets Lowest priority

### Android Intent
- A simple message object which is used by activity to go from one to another

- The following  tasks are performed using Intent:
 - Start a new Activity or Service from the current Activity
 - Pass data between Activities and Services

## Services
- Runs in background without user interface
- Uses broadcast receivers to communicate
- Notifies users through notification framework
- Runs in hosting process’s main thread unless specified to create its own thread
- Recommended to run in own thread for CPU intensive tasks, E.g. File download
- Also consider to use AsyncTask, HandlerThread instead of Service for CPU intensive work

## Service States
### Started State 
- Application components start service by calling startService() method
- Runs indefinitely in the background even if the component that started destroyed
- Stops itself once the task is completed or can be stopped by the component

### Bound State
- Application components start service by calling bindService() method
- Provides client-server like interface, so component can directly call service methods and get results
- More than one component can bind the same service
- Destroyed when no component bound to it


## Broadcast Receivers
- A component used to register for system wide or applications broadcast events
- Notified by Android system when broadcast event occur
- Not providing user interface, may notify the users through notification status message
- There are two kind of events
    System level events : Originated from Android system
        E.g.  Boot completed, power connected, power disconnected, battery low, battery OK
    Application level events: Originated from user application 
        E.g. MP3 file downloaded, Picture captured
        
- Life cycle : Once the onReceive() of the receiver class has finished, the Android system recycles the receiver
- Able to work even if the application is not running

## Content Provider
- App can store data locally in the file systems, SQLite database
- App runs in its own process and permission that keeps data hidden from another app
- Content Providers offers data sharing between apps
- Contacts app provides data multiple apps such as SMS app, Dialer app etc
- Data can be queried, modified, deleted and added through Content Provider

## Android Manifest
- Describes components and settings of an app
- File placed in root of the project structure
- Read by the system during startup
- ovides Java package name which is an unique identifier of an app
- scribes the components – Activities, Services, Broadcast receivers, Content providers
- fines various permissions
- Declares required minimum level of API 





