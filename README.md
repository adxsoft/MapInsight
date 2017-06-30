# MapInsight

## Version History
v1.0 First Version

## Installation
1. Open the User Directory in Freeplane (Tools/Open User directory)
2. Open the scripts folder
3. Save this script as 'MapInsight_V0_1.groovy' in the scripts folder
4. Restart Freeplane

To Use Script
1. Select an existing node
2. In Freeplane 'Tools' menu select 'Scripts'
3. Choose the script 'MapInsight_V1_0'

## Introduction
MapInsight is a Freeplane script I have been experimenting with which provides a simple UI for walking around a map and optionally creating connectors 
This script has been tested on Freeplane 1.3 and 1.5 on Windows and Mac.

I do use connectors widely and found that a map can get very confusing when there are lots of connectors. 
This simple UI gives the view from a selected node showings its parent, children, connectors in and out. 
Hovering over a node in the UI reveals the notes and details. 

Connection Canidates tool finds possible connections through link words or phrases and allows you to create the connector

Connection Manager shows all connectors in the map in a sortable table with edit/delete options

Hopefully this script will run unchanged on newer versions of Freeplane.

## 1. WALKING AROUND NODES
This script creates a free standing resizable window that allows you to 'walk' around
the nodes in a map.

When the script is run the currently selected node in the map is displayed along with its related nodes
ie. parent, children, connections in and out.

The user can double click any of the related nodes and it will be selected and become the currently selected node

Hovering over any related nodes shows its note and detail text if present.

To see the Note and details of the currently selected node use the View Menu and select Node Details

To see recent nodes visited see the History menu item

## 2. FINDING ASSOCIATED NODES

The script shows a sortable list of any possible related nodes (candidate nodes) by using 'proper'
words
(a) from the selected node's core text
(b) optionally from the node's note text
(c) optionally from the node's detail text
(d) and/or words entered by the user (separated by commas) or you can
    specify a search phrase which is any string inside double quotes

For example if the selected node had the word 'London'
then any other nodes in map with the word 'London' in them would show as a possible connections.

(Proper word means a word is not a noise or stop word such as and, or, if etc.
 This script has English stop words - can alter by changing 'stopWords' table in script)

If a candidate node is selected it will turn blue.

Once selected you can view the node in the map without selecting it (locate button) or
select the node (Go To button) and Map Insight will 'walk' to that node.

If a candidate node is right clicked then you can choose to add a connector between the main node and
the selected candidate node. The connector is created with the middle label being the word that links
the main and candidate node (eg 'london' in the example above).

If you wish to automatically create a connector for ALL the candidates then
press the "Connect All" button. You can reverse this by clicking the "Undo Connect ALL" button.

## 3. MANAGING CONNECTORS

Shows all the connectors in a sortable list and allows you to remove them or change the label if required.

This is useful when a map has so many connectors that they are difficult to see in the map

First select the base node to see the connectors belonging to it and its sub nodes.
Selecting the root node will view all connectors in the map.

Choose 'Connectors Manager' in the View menu

All connectors will be shown with the source node, target node and middle label. (To avoid label confusion
I decided to just use middle labels).

Clicking on a connector will highlight the connector in BLUE in the list and BLUE in the map.

You can remove the connector by clicking the 'Remove Connector' button

You can change the connectors middle label by clicking the 'Edit Label'

End of Document
