# DSync
Decentralized dataset state synchronization protocol utilizing NDN.

DSync is meant to be tested with https://github.com/gpwclark/chronosync-chat-simulation. 
DSync allows the synchronization of a shared dataset over and in the chat simlation
does so using a chat room as a shared dataset.

DSync works by passing around a 'Rolodex' this rolodex is a serialized object that represents
all of the contacts currently in the chat room. This rolodex is passed around and users add 
themselves to it when they join the chat room. DSync expresses interest in all of the data
other user's add and it knows the data names because they are derived from the rolodex. 
When users "send" a chat message. That messsage is given to dsync. DSync answers requests
for that data from other users and passes that data to a callback the consumer provides DSync,
in the chat simulation example, this is ChronoChat.

More work needs to be done to support users leaving, and changing their data names. More work also
needs to be done to handle network partition as well as necomer's coming to the chat room and getting
caught up.
