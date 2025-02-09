
### STATUS
~~~
            GENERAL 
        
    *SERVER*
    
OK - action was completed successfuly 
FAIL - action was not completed sucessfuly
EXPIRED - token is expired and user is logged out

    *CLIENT*
OK - just checking up and telling that everything is ok
  

            IN-GAME 
            
            
    *CLIENT*
NO_STATE - client has record of being in a game but does not have state of said game
~~~ 


### ACTIONS
~~~
* - Optional arguments

AUTH
LOGIN
    CLIENT
        ARGS
            USERNAME
            PASSWORD
            
    SERVER
        ARGS
            STATUS
                - OK : creates and returns token 
                - FAIL : waits for another login
            MESSAGE
            TOKEN
        
LOGOUT
    CLIENT    
        ARGS
            TOKEN
    SERVER
        ARGS
            STATUS
                - OK : token is expired
                - FAIL : token didnt exist
                - EXPIRED : token was already expired
    
PING
    CLIENT
        ARGS
            STATUS
                - OK : everything ok, waits for server OK
                - NO_STATE : telling the server that it needs data about the game it was previously connected
            TOKEN
            *GAME_ID : usually paired with NO_STATE
            
            
         
    SERVER
        ARGS
            STATUS
                - OK : token expiry time was extended
                - EXPIRED : token is expired
                - RECONNECT : client was lost, waits for NO_STATE or OK
                
    - OK : 
    - FAIL : 
    - EXPIRED : 
    - RECONNECT :


DISCONNECTED 
~~~





