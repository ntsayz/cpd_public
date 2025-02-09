### Instructions to run the project

1. **Compile the Project**:
#### Unix
```sh
    cd src # From the current directory
```
```sh
    mkdir -p out/production/Client out/production/Server out/production/Shared
```
```sh
    javac -cp lib/json-20240303.jar -d out/production src/Shared/User.java src/Client/ClientConnect.java src/Client/ClientGUI.java src/Server/*.java src/Main.java
```

#### Windows
```sh
cd src
```
```sh
mkdir out\production\Client out\production\Server out\production\Shared
```
```sh
javac -cp lib\json-20240303.jar -d out\production src\Shared\User.java src\Client\ClientConnect.java src\Client\ClientGUI.java src\Server\*.java src\Main.java

```

1. **Run the Application**:

The programs select by default the port 9000 to communicate, if this port is occupied in your computer , it might be a good idea to include the port that is available in your computer 

### Unix

- Run the server:

```sh
java -cp lib/json-20240303.jar:out/production Server.GameServer <optional_port> 
```

- Run the Client:

```sh
java -cp lib/json-20240303.jar:out/production Client.ClientConnect <optional_port>
```


### Windows

- Run the server:

```sh
java -cp lib\json-20240303.jar;out\production Server.GameServer <optional_port> 
```

- Run the Client:

```sh
java -cp lib\json-20240303.jar;out\production Client.ClientConnect <optional_port>
```
