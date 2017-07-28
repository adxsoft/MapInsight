scriptVersion='MapInsight v1.1'

helptext1="""
$scriptVersion
28th July 2017

1. WALKING AROUND NODES
This script creates a free standing resizable window that allows you to 'walk' around
the nodes in a map.

When the script is run the currently selected node in the map is displayed along with its related nodes
ie. parent, children, connections in and out.

The user can double click any of the related nodes and it will be selected and become the currently selected node

Hovering over any related nodes shows its note and detail text if present.

To see the Note and details of the currently selected node use the View Menu and select Node Details

To see recent nodes visited see the History menu item
"""

helptext2="""

2. FINDING ASSOCIATED NODES

The script shows a sortable list of any possible related nodes (candidate nodes) by using 'proper'
words
(a) from the selected node's core text
(b) optionally from the node's note text
(c) optionally from the node's detail text
(d) and/or words entered by the user (separated by commas) or you can
    specify a search phrase which is any string inside double quotes
(e) or a regular expression which is any string inside forward slashes 
    eg /M.*h/ would find the text March, Macbeth. Moth

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
"""

helptext3="""
3. MANAGING CONNECTORS

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

4. VIEWING CONNECTORS

The sub menu in the View menu lets you view specific connectors

'Show Current Node Connectors' will display only connectors to and from the current node in the map. All other
connectors will be hidden

'Show Connectors for a label' will display only connectors with a specific label in the map. All other
connectors will be hidden

'Hide all Connectors' will hide all connectors in the map (useful in crowded maps)

'Unhide all Connectors' will show all connectors in the map

"""

versionhistory="""
Version History
v1.0 First Version
v1.1 Added following new features thanks to forum members feedback
1. 'Refresh current view' in the Actions menu. Updates the current node details which is handy when underlying map has changed
2. New button 'Save As Map' on 'Possible Connections' window will save the source node and all suggested nodes together 
   with the connectors into a new map
3. Connector Candidates Options windows now caters for regular expressions
4. View, Connectors has new menu item 'Show Current Node Connectors' which will display only the connectors for the 
   current node and hide all other connectors. 'Unhide All Connectors' will restore view of all connectors 
5. View, Connectors has new menu item 'Show Connectors for a label' which will display only the connectors with a
   middle label that contains the label text and hide all other connectors. 'Unhide All Connectors' will restore view of all connectors 
6. Allows multiline labels when adding connectors
7. Bug fixes (as per https://sourceforge.net/p/freeplane/discussion/758437/thread/a49efba6/)
"""

installation="""
Installation
1. Open the User Directory in Freeplane (Tools/Open User directory)
2. Open the scripts folder
3. Save this script as 'Map Insight v0.1.groovy' in the scripts folder
4. Restart Freeplane

To Use Script
1. Select an existing node
2. In Freeplane 'Tools' menu select 'Scripts'
3. Choose the script 'Map_Insight_V1_0'
"""

import javax.swing.*
import java.awt.*
import groovy.swing.SwingBuilder
import javax.swing.table.*
import java.awt.event.*
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

//*********************************************
//         Global Variables
//*********************************************

newNode=null                        // refers to the node that Map Insight is focussed on
// (central white node in the Map Insight window

def recenthistoryDisplayed=false        // stops recent history being displayed twice

def selectedCandidateNode=null          // refers to a node that may be a connector candidate

def selectedConnectortablerow=null      // refers to a connector table row that is currently selected in the Connectors window

def selectedconnectorobject=null        // refers to the connector object that is currently selected in the Connectors window

def currentconnectorobjects=[]          // buffr used to store all connectors for cut and paste

//*********************************************
//         Global Functions
//*********************************************

// These global function closures are added to the main groovy script object (Object.metaClass)
// so they can be used globally through the script
Object.metaClass.msg = {text -> msg(text)}
Object.metaClass.statusBarMsg = {msg -> statusBarMsg(msg)}
Object.metaClass.getNodeFromMapSelection = {-> getNodeFromMapSelection()}
Object.metaClass.getNodeByID = {nodeID -> getNodeByID(nodeID)}
Object.metaClass.findNodeByID = {nodeID -> findNodeByID(nodeID)}
Object.metaClass.selectNodeByID = {nodeID -> selectNodeByID(nodeID)}
Object.metaClass.formatForSwingDisplay = {nodetype,text,note,details -> formatForSwingDisplay(nodetype,text,note,details)}
Object.metaClass.removeHtmlTags = {text -> removeHtmlTags(text)}
Object.metaClass.removeHtmlTags = {-> setAllConnectorsToDefaultColor()}
Object.metaClass.showConnectorsForCurrentNode = {-> showConnectorsForCurrentNode()}
Object.metaClass.hideAllConnectors = {-> hideAllConnectors()}
Object.metaClass.unhideAllConnectors = {-> unhideAllConnectors()}
Object.metaClass.formatNodeTextForCell = {nodetext ->formatNodeTextForCell(nodetext)}
Object.metaClass.removeAllConnectors = {->removeAllConnectors()}

//-----------------------
// == GLOBAL FUNCTION: == display text as Freeplane information message
def msg(text) {
    ui.informationMessage(text)
}


//-----------------------
// == GLOBAL FUNCTION: == display text on status bar
def statusBarMsg(msg) {
    c.statusInfo=msg
}

//-----------------------
// == GLOBAL FUNCTION: == get node object for currently selected node
def getNodeFromMapSelection() {
    def theNode=null
    c.selected.each{
        theNode=it
    }
    return theNode
}

//-----------------------
// == GLOBAL FUNCTION: == get node object for specific node ID
def getNodeByID(nodeID) {
    def theNode=null
    c.find{it.nodeID==nodeID}.each{
        theNode=it
    }
    return theNode
}

//-----------------------
// == GLOBAL FUNCTION: == find node object for specific node ID
def findNodeByID(nodeID) {
    def theNode=null
    c.find{it.nodeID==nodeID}.each{
        theNode=it
    }
    return theNode
}

//-----------------------
// == GLOBAL FUNCTION: == select node on Map by node ID and center the map on the node
def selectNodeByID(nodeID) {
    c.find{it.nodeID==nodeID}.each{
        c.select(it)
        c.centerOnNode(it)
        updateRecentNodesVisited(it.text)
    }

}

//-----------------------
// == GLOBAL FUNCTION: == select node object with specific title and center the map on the node
def selectNodeByTitle(nodeTitle) {
    c.find{it.text==nodeTitle}.each{
        c.select(it)
        c.centerOnNode(it)
        updateRecentNodesVisited(it.text)
    }

}

//-----------------------
// == GLOBAL FUNCTION: == copy a node and all its properties to a new node
def copyProperties(dest, source) {
    dest.text = source.text
    dest.attributes = source.attributes.map
    dest.link.text = source.link.text
    if (source.note != null)
        dest.note = source.note
    dest.details = source.detailsText
}



// initialise recent nodes visited array
recentNodesVisited=[]

//-----------------------
// == GLOBAL FUNCTION: == update recent nodes visited array
def updateRecentNodesVisited(newNodeTitle) {
    if (!recentNodesVisited.contains(newNodeTitle)) {
        if (recentNodesVisited.size()>15) recentNodesVisited.pop()
        recentNodesVisited.add(0,newNodeTitle)
    }
}

//-----------------------
// == GLOBAL FUNCTION: == prepare node core, note and detail texts for display in Swing UI elements
def formatForSwingDisplay(nodetype,text,note,details) {
    // In order to display node, note and detail text
    // in swing ui elements remove extraneous html tags
    // and format with 'clean' HTML to keep Swing UI
    // elements happy

    text=nodetype
    wrapafterchars=80
    wrapcharacter='\n'
    String msgtext='<HTML><head></head><body  style=\"width: 250px;\">'
    if (text==null) text='empty'
    text=text
            .replace('</html>','')
            .replace('</HTML>','')
            .replace('<html>','')
            .replace('<HTML>','')
            .replace('</head>','')
            .replace('</HEAD>','')
            .replace('<head>','')
            .replace('<HEAD>','')
            .replace('</body>','')
            .replace('</BODY>','')
            .replace('<body>','')
            .replace('&#160;','&nbsp;')
    text=wordwrap(text,wrapafterchars,wrapcharacter).trim()
    msgtext+="<B><font color=\"blue\">Title:</font></B><HR><B>$text</B><BR>"
    if (note==null) note='empty'
    note=note
            .replace('</html>','')
            .replace('</HTML>','')
            .replace('<html>','')
            .replace('<HTML>','')
            .replace('</head>','')
            .replace('</HEAD>','')
            .replace('<head>','')
            .replace('<HEAD>','')
            .replace('</body>','')
            .replace('</BODY>','')
            .replace('<body>','')
            .replace('&#160;','&nbsp;')
    note=wordwrap(note,,wrapafterchars,wrapcharacter).trim()
    msgtext+="<B><font color=\"blue\">Note:</font></B><HR>$note<BR><BR>"
    if (details==null) details='empty'
    details=details
            .replace('</html>','')
            .replace('</HTML>','')
            .replace('<html>','')
            .replace('<HTML>','')
            .replace('</head>','')
            .replace('</HEAD>','')
            .replace('<head>','')
            .replace('<HEAD>','')
            .replace('</body>','')
            .replace('</ BODY>','')
            .replace('<body>','')
            .replace('&#160;','&nbsp;')
    details=wordwrap(details,wrapafterchars,wrapcharacter).trim()
    msgtext+="<B><font color=\"blue\">Details:</font></B><HR>$details<BR>"
    msgtext+="""</body></HTML>"""

    return msgtext.replace('\n\n','<BR>').replace('\n','<BR>')
}


//-----------------------
// == GLOBAL FUNCTION: == word wrap text
def wordwrap(text, width=80, prefix='') {
    def out = ''
    def remaining = text.replaceAll("\n", " ")
    while (remaining) {
        def next = prefix + remaining
        def found = next.lastIndexOf(' ', width)
        if (found == -1) remaining = ''
        else {
            remaining = next.substring(found + 1)
            next = next[0..found]
        }
        out += next + '\n'
    }
    return out
}

//-----------------------
// == GLOBAL FUNCTION: == strip HTML tags from text
def removeHtmlTags(text) {
    if (text!=null) {
        def strippedText = text.replaceAll('\n\\s*', '\n') // remove extra spaces after line breaks
        strippedText = strippedText.replaceAll('<.*?>', '') // remove anythiing in between < and >
        strippedText = strippedText.replaceAll('^\\s*', '') // remove whitespace
        strippedText = strippedText.replaceAll('\n\n\n', '\n') // replace multiple line feed with single line feed
        return strippedText
    } else return ""
}


//-----------------------
// == GLOBAL FUNCTION: == format text for display in table cell
def formatNodeTextForCell(nodetext) {
    maxCharsInCell=100
    nodetext=removeHtmlTags(nodetext).take(maxCharsInCell)
            .replace('&#160;','')
    return nodetext
}


//-----------------------
// == GLOBAL FUNCTION: == clean up a word for comparisons
def cleanupWord(word) {
    cleanword=word.toLowerCase()
            .replace('.','') // remove full stops
            .replace(',','') // remove commas
            .replace('!','') // remove exclamation marks
            .replace("'s",'') // remove plurals
            .trim()          // remove leading and trailing blanks
    if (cleanword.endsWith('s')) { // remove plural from words ending in s and NOT ss
        if (!cleanword.endsWith('ss')) {
            cleanword=cleanword.substring(0,cleanword.length()-1)
        }
        if (cleanword.endsWith('&')) {
            cleanword=cleanword.substring(0,cleanword.length()-1)
        }
    }
    return cleanword
}

//-----------------------
// == GLOBAL FUNCTION: == set all connectors in the current node and subnodes to default color (GRAY)
def setAllConnectorsToDefaultColor() {
    // set all connectors to GRAY
    node.map.root.findAll().each {
        it.connectorsOut.each {
            it.setColor(Color.GRAY)
        }
    }
}

//-----------------------
// == GLOBAL FUNCTION: == remove  All connectors in the current node and subnodes to default color (GRAY)
def removeAllConnectors() {
    // set all connectors to GRAY
    node.map.root.findAll().each {
        it.connectorsIn.each {
            node.removeConnector(it)
        }
        it.connectorsOut.each {
            node.removeConnector(it)
        }
    }
}

//-----------------------
// == GLOBAL FUNCTION: == hide all connectors by setting color to WHITE
def hideAllConnectors() {
    // set all connectors to WHITE
    node.map.root.findAll().each {
        it.connectorsOut.each {
            it.setColor(Color.WHITE)
        }
    }
}

// == GLOBAL FUNCTION: == unhide all connectors by setting color to default
def unhideAllConnectors() {
    setAllConnectorsToDefaultColor()
}

// == GLOBAL FUNCTION: == unhide all connectors by setting color to default
def showConnectorsForCurrentNode() {
    // TODO Only show connectors in map for current node
    hideAllConnectors()
    // set current node connectors to GRAY
    newNode.connectorsOut.each {
        it.setColor(Color.GRAY)
    }
    newNode.connectorsIn.each {
        it.setColor(Color.GRAY)
    }
}

//-----------------------
// == GLOBAL FUNCTION: == set all connectors in the current node and subnodes to default color (GRAY)
def showAllConnectorsWithLabel(label) {
    def searchtype="contains"
    def searcharg=label
    if (label.startsWith('/') && label.endsWith('/')) {
        searchtype="regex"
        searcharg=label.substring(1,label.length()-1)
    }

    // set all connectors to GRAY
    hideAllConnectors()
    node.map.root.findAll().each {
        it.connectorsIn.each {
            if (searchtype=="contains") {
                if (it.middleLabel.toLowerCase().contains(searcharg.toLowerCase())) {
                    it.setColor(Color.RED)
                }
            }
            if (searchtype=="regex") {
                if (it.middleLabel=~searcharg) {
                    it.setColor(Color.BLUE)
                }
            }
        }
        it.connectorsOut.each {
            if (searchtype=="contains") {
                if (it.middleLabel.toLowerCase().contains(searcharg.toLowerCase())) {
                    it.setColor(Color.RED)
                }
            }
            if (searchtype=="regex") {
                if (it.middleLabel=~searcharg) {
                    it.setColor(Color.BLUE)
                }
            }
        }
    }
}


//-----------------------
// == GLOBAL FUNCTION: == populate the working array with
// (1) the parent node details
// (2) children node details
// (3) connections in/out details
def loadNodeData(node) {
    // load node data
    //

    // get parent node details
    def nodeparenttext = 'root'
    def nodeparentnotetext = ''
    def nodeparentdetailstext = ''
    def nodeparentID = null

    if (node.parent != null) {
        nodeparenttext = node.parent.text
        nodeparentnotetext = node.parent.noteText
        nodeparentdetailstext = node.parent.detailsText
        nodeparentID = node.parent.nodeID
    }

    // clear working array
    nodes_data = []

    // add parent node info of selected node to working array
    nodes_data.add([id: nodeparentID, type: 'parent', nodetext: nodeparenttext, label: 'parent', notetext: nodeparentnotetext, details: nodeparentdetailstext])

    // add child node(s) info of selected node to working array
    if (node.children) {
        node.children.each {
            nodes_data.add([id: it.nodeID, type: 'child', nodetext: it.text, label: 'child', notetext: it.noteText, details: it.detailsText])
        }
    }

    // add info for any connections (source nodes) into this selected node into the working array
    if (node.connectorsIn) {
        node.connectorsIn.each {
            if (node != it.source) {
                def middleLabel = "<-"
                if (it.middleLabel != null) {
                    middleLabel = it.middleLabel
                }
                sourceNode = getNodeByID(it.delegate.source.id)
                nodes_data.add([id: it.source.nodeID, type: 'conn-IN', nodetext: it.source.text, label: middleLabel, notetext: sourceNode.noteText, details: sourceNode.detailsText])
            }
        }
    }

    // add info for any connections out (target nodes) from this selected node into the working array
    if (node.connectorsOut) {
        node.connectorsOut.each {
            if (node != it.target) {
                def middleLabel = "->"
                if (it.middleLabel != null) {
                    middleLabel = it.middleLabel
                }
                targetNode = getNodeByID(it.delegate.targetID)
                targetnoteText = targetNode.noteText.toString()
                nodes_data.add([id: it.target.nodeID, type: 'conn-OUT', nodetext: it.target.text, label: middleLabel, notetext: targetnoteText, details: targetNode.detailsText])
            }
        }
    }
    updateRecentNodesVisited(node.text)
    newNode=node
}

//-----------------------
// == GLOBAL FUNCTION: == checks if the nodeID is not present in the working arrays which are visible in the ui Tables
def nodeIDNotInCurrentTables(nodeID) {
    def result=true
    if (connection_candidates_nodes_data.find { it.id==nodeID }) result=false
    return result
}


//-----------------------
// == GLOBAL FUNCTION: == Update the user interface (UI) tables from the working array for the selected node
def updateUI (newNode) {
    selected_node_button.text=formatNodeTextForCell(newNode.text)
    selected_node_button.setToolTipText(formatSelectedNodeButtonToolTipText(newNode))
    frame.title="$scriptVersion"
    connectorsInTable.model = swing.tableModel(list: nodes_data.findAll{it.type=="conn-IN"}) {
        propertyColumn(header: 'Type', propertyName: 'type', editable: false, cellRenderer: new MainTableCellRenderer())
        propertyColumn(header: 'Node', propertyName: 'nodetext', editable: false, cellRenderer: new MainTableCellRenderer())
        propertyColumn(header: 'Note', propertyName: 'notetext', editable: false, cellRenderer: new MainTableCellRenderer())
        propertyColumn(header: 'Details', propertyName: 'details', editable: false, cellRenderer: new MainTableCellRenderer())
        propertyColumn(header: 'Label', propertyName: 'label', editable: false, cellRenderer: new MainTableCellRenderer())
    }
    parentTable.model = swing.tableModel(list: nodes_data.findAll{it.type=="parent"}) {
        propertyColumn(header: 'Type', propertyName: 'type', editable: false, cellRenderer: new MainTableCellRenderer())
        propertyColumn(header: 'Node', propertyName: 'nodetext', editable: false, cellRenderer: new MainTableCellRenderer())
        propertyColumn(header: 'Note', propertyName: 'notetext', editable: false, cellRenderer: new MainTableCellRenderer())
        propertyColumn(header: 'Details', propertyName: 'details', editable: false, cellRenderer: new MainTableCellRenderer())
        propertyColumn(header: 'Label', propertyName: 'label', editable: false, cellRenderer: new MainTableCellRenderer())
    }
    childrenTable.model = swing.tableModel(list: nodes_data.findAll{it.type=="child"}) {
        propertyColumn(header: 'Type', propertyName: 'type', editable: false, cellRenderer: new MainTableCellRenderer())
        propertyColumn(header: 'Node', propertyName: 'nodetext', editable: false, cellRenderer: new MainTableCellRenderer())
        propertyColumn(header: 'Note', propertyName: 'notetext', editable: false, cellRenderer: new MainTableCellRenderer())
        propertyColumn(header: 'Details', propertyName: 'details', editable: false, cellRenderer: new MainTableCellRenderer())
        propertyColumn(header: 'Label', propertyName: 'label', editable: false, cellRenderer: new MainTableCellRenderer())
    }
    connectorsOutTable.model = swing.tableModel(list: nodes_data.findAll{it.type=="conn-OUT"}) {
        propertyColumn(header: 'Type', propertyName: 'type', editable: false, cellRenderer: new MainTableCellRenderer())
        propertyColumn(header: 'Node', propertyName: 'nodetext', editable: false, cellRenderer: new MainTableCellRenderer())
        propertyColumn(header: 'Note', propertyName: 'notetext', editable: false, cellRenderer: new MainTableCellRenderer())
        propertyColumn(header: 'Details', propertyName: 'details', editable: false, cellRenderer: new MainTableCellRenderer())
        propertyColumn(header: 'Label', propertyName: 'label', editable: false, cellRenderer: new MainTableCellRenderer())
    }
    setColumnsForDisplay()
    setConnectorLabelsForDisplay()
    frame.pack()
    frame.show()
}

// ===============================================================
// ====================== UI Functions ===========================
// ===============================================================

// this class overrides the standard table cell renderer in the tables
// populated from the working arrays
// Each table cell contains a nodes core text. A tooltip displays
// the nodes note and detail text

class MainTableCellRenderer extends JLabel implements TableCellRenderer {
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int rowIndex, int vColIndex) {


        //*********************************************
        // set the text for the cell
        //*********************************************
        //def displaytext=formatNodeTextForCell(value.toString())
        setText(value.toString())


        // set the cell to text and connector label (if row is showing a connector OUT)
        if (table.model.getValueAt(rowIndex,0)=='conn-OUT') {
            setText('(' + table.model.getValueAt(rowIndex, 4) + ') '+removeHtmlTags(value.toString()))
        }

        // set the cell to text and connector label (if row is showing a connector IN)
        if (table.model.getValueAt(rowIndex,0)=='conn-IN') {
            setText(removeHtmlTags(value.toString() + ' (' + table.model.getValueAt(rowIndex, 4)) + ')')
        }

        // center the text in the cell
        setHorizontalAlignment(SwingConstants.CENTER)

        // color the cell based on whether the referred node is
        // a parent of the selected node
        // a child of the selected node
        // a connection into or out of the selected node
        if (table.model.getValueAt(rowIndex,0)=='parent') setForeground(new Color(0,102,0))
        if (table.model.getValueAt(rowIndex,0)=='child') setForeground(Color.BLUE)
        if (table.model.getValueAt(rowIndex,0)=='conn-IN') setForeground(Color.RED)
        if (table.model.getValueAt(rowIndex,0)=='conn-OUT') setForeground(new Color(153,0,0))
        if (vColIndex==0) {
            setForeground(Color.WHITE)
        }
        //*****************************************************
        // Set up tooltip for cell which shows the note and
        // detail texts related to the node the cell refers to
        //*****************************************************


        // show connector labels for connectors in and out in the tooltip
        String nodetype=removeHtmlTags(value.toString())+'<HR>'+table.model.getValueAt(rowIndex,0)+'<HR>'
        if (table.model.getValueAt(rowIndex,0)=='conn-IN' || table.model.getValueAt(rowIndex,0)=='conn-OUT') {
            nodetype=removeHtmlTags(value.toString())+'<HR>'+table.model.getValueAt(rowIndex,0)+' ('+table.model.getValueAt(rowIndex,4)+')'+'<HR>'
        }

        // show the node's note text
        String notetext=table.model.getValueAt(rowIndex,2)
        if (notetext==null) notetext="empty"

        // show the node's details text
        String detailstext=table.model.getValueAt(rowIndex,3)
        if (detailstext==null) detailstext="empty"

        // format the tooltip for display
        def tooltiptext= "<html><br>" + formatForSwingDisplay(nodetype,value,notetext,detailstext)

        // attach tooltip to the cell
        setToolTipText((String) tooltiptext)

        return this
    }
}

//-----------------------
// == UI FUNCTION: == display the details for a node selected in a UI table
def displayNewNode(tab,nodes_data) {
    def nodedata=nodes_data[tab.getSelectedRow(),tab.getSelectedColumn()][0]
    def theNode=getNodeByID(nodedata["id"])
    selectNodeByID(nodedata["id"])
    loadNodeData(theNode)
    updateUI(theNode)
}

//-----------------------
// == UI FUNCTION: == display the details for the source and target notes for a node candidate
def displayConnection(tab,nodes_data,candidatenodeID) {
    def nodedata=nodes_data[tab.getSelectedRow(),tab.getSelectedColumn()][0]
    def a=1
//    def theNode=getNodeByID(nodedata["id"])
//    selectNodeByID(nodedata["id"])
//    loadNodeData(theNode)
//    updateUI(theNode)
}

//-----------------------
// == UI FUNCTION: == Set the display widths for the UI
def setColumnsForDisplay() {

    // set width of parent table which forces panel to the minWidth value
    minWidth=300
    parentTable.getColumn('nodetext').setMinWidth(minWidth)

    HideTableColumns(parentTable,['type','notetext','details','label'])
    HideTableColumns(childrenTable,['type','notetext','details','label'])
    HideTableColumns(connectorsInTable,['type','notetext','details','label'])
    HideTableColumns(connectorsOutTable,['type','notetext','details','label'])
}

//-----------------------
// == UI FUNCTION: == Hide one or more table columns
def HideTableColumns(table,columns) {
    columns.each {
        try {
            table.getColumn(it).setWidth(0)
            table.getColumn(it).setMinWidth(0)
            table.getColumn(it).setMaxWidth(0)
        } catch(all) {
            // ignore if column not found
        }
    }
}

//-----------------------
// == UI FUNCTION: == Set the connection labels based on whether connections or children are present for the selected node
def setConnectorLabelsForDisplay() {
    if (nodes_data.find{it.type=="conn-IN"}) {
        conn_in_label.setVisible(true)
    } else {
        conn_in_label.setVisible(false)
    }
    if (nodes_data.find{it.type=="conn-OUT"}) {
        conn_out_label.setVisible(true)
    } else {
        conn_out_label.setVisible(false)
    }
    if (nodes_data.find{it.type=="child"}) {
        children_label.setVisible(true)
    } else {
        children_label.setVisible(false)
    }
}

//-----------------------
// == UI FUNCTION: == Set the connection labels based on whether connections or children are present for the selected node
def setButtonsForDisplay() {
    selected_node_button.setToolTipText(formatSelectedNodeButtonToolTipText(newNode))
}

//-----------------------
// == UI FUNCTION: == format tooltip for the selected node button
def formatSelectedNodeButtonToolTipText(selectednode) {
    return "<html><b>Selected Node</b><br><br>" +
            formatForSwingDisplay(selectednode.text + '<br>',
                    selectednode.text,
                    selectednode.noteText,
                    selectednode.detailsText).replace('<html>', '')
}



//*********************************************
//*********************************************
//*********************************************
//         MAIN LOGIC ENTRY POINT
//*********************************************
//*********************************************
//*********************************************

// load the data from the currently selected node
loadNodeData(node)

Color tablebg = new Color(224,224,224)


// ===============================================================
// ============== THE USER INTERFACE DEFINITION   ================
// ===============================================================

//         BASIC LAYOUT FOR DISPLAYING SELECTED NODE
//                 -------------------------
//                 | Menus                 |
//                 -------------------------
//                 |                       |
//                 |      PARENT NODE      |
//                 |          v            |
//                 |       CONNECTED       |
//                 |         NODES         |
//                 |          IN           |
//                 |          v            |
//                 |     ------------      |
//                 |     | SELECTED |      |
//                 |     |   NODE   |      |
//                 |     ------------      |
//                 |          v            |
//                 |       CONNECTED       |
//                 |         NODES         |
//                 |         OUT           |
//                 |          v            |
//                 |      CHILD NODES      |
//                 |                       |
//                 -------------------------

// create a groovy SwingBuilder
swing = new SwingBuilder()

// define the main ui window(frame)
frame = swing.frame(title: "$scriptVersion", defaultCloseOperation: JFrame.DISPOSE_ON_CLOSE, alwaysOnTop: true,minimumSize: new Dimension(400,25)) {
    // main panel
    mypanel = panel (background: tablebg) {
        gridLayout()
        vbox {

            // Menu panel
            panel(background: tablebg) {
                gridLayout()
                mainmenu=menuBar(minimumSize: new Dimension(400,25),maximumSize: new Dimension(400,25)) {
                    menu(text:'Actions') {
                        menuItem() {
                            action(
                                    name:"open selected map node", closure:{
                                newNode = getNodeFromMapSelection()
                                loadNodeData(newNode)
                                updateUI(newNode)
                            })
                        }
                        separator()
                        menuItem() {
                            action(
                                    name:"Go to Root Node", closure:{
                                newNode=newNode.map.root
                                loadNodeData(newNode.map.root)
                                updateUI(newNode.map.root)
                            })
                        }
                        menuItem() {
                            action(
                                    name:"Refresh current view", closure:{
                                loadNodeData(newNode)
                                updateUI(newNode)
                            })
                        }
                        separator()
                    }
                    menu(text:'View') {
                        menuItem() {
                            action(name:"Node Details",
                                    closure:{
                                        if (newNode!=null) {
                                            def notetext='empty'
                                            if (newNode.noteText!=null) {
                                                notetext=newNode.noteText
                                            }
                                            def detailstext='empty'
                                            if (newNode.detailsText!=null) {
                                                detailstext=newNode.detailsText
                                            }
                                            msgtext=formatForSwingDisplay(newNode.text,newNode.text,notetext,detailstext)
                                            msg=label(msgtext)
                                            def pane = swing.optionPane(message: msg)
                                            def dialog = pane.createDialog(frame, 'Note')
                                            dialog.show()
                                        }
                                    })
                        }
                        separator()
                        menu(text:'Connectors') {
                            menuItem() {
                                action(name:"Connector Candidates",
                                        closure:{
                                            connectoroptions()
                                        })
                            }
                            separator()
                            menuItem() {
                                action(name: "Connectors Manager",
                                        closure: {
                                            connectorManagerUI()
                                        })
                            }
                            separator()
                            menuItem() {
                                action(name: "Show Current Node Connectors",
                                        closure: {
                                            if (newNode!=null) {
                                                showConnectorsForCurrentNode()
                                            }
                                        })
                            }
                            separator()
                            menuItem() {
                                action(name: "Show Connectors for a label",
                                        closure: {
                                            def pane = swing.optionPane()
                                            def label = pane.showInputDialog(null,"Enter full or partial label text\n OR a regular expression eg /M.*h/","Connector Label Search",JOptionPane.QUESTION_MESSAGE)
                                            showAllConnectorsWithLabel(label)
                                        })
                            }
                            separator()
                            menuItem() {
                                action(name: "Hide All Connectors",
                                        closure: {
                                            hideAllConnectors()
                                        })
                            }
                            separator()
                            menuItem() {
                                action(name: "UnHide All Connectors",
                                        closure: {
                                            unhideAllConnectors()
                                        })
                            }
                            separator()
                        }
                    }
                    menu(text:'History') {
                        menuItem() {
                            action(name:'Recent Nodes Visited',
                                    closure:{
                                        if (recenthistoryDisplayed==true || recentNodesVisited.size==0) {
                                            return
                                        }
                                        recenthistoryDisplayed=true
                                        swing.setVariable('Recent Nodes Visited',[:])
                                        def vars = swing.variables
                                        dial = swing.dialog(title:'Recent Nodes Visited',
                                                id:'recentDialog',
                                                minimumSize: [300,50],
                                                modal:true,
                                                location: [mypanel.getAt('width')+5,0],
//                                                locationRelativeTo: ui.frame,
                                                owner:ui.frame,
                                                defaultCloseOperation:JFrame.DO_NOTHING_ON_CLOSE,
                                                //                    Using DO_NOTHING_ON_CLOSE so the Close button has full control
                                                //                    and it can ensure only one instance of the dialog appears
                                                pack:true,
                                                show:true) {
                                            panel() {
                                                boxLayout(axis: BoxLayout.Y_AXIS)
                                                recentspanel=panel(alignmentX: 0f) {
                                                    flowLayout(alignment: FlowLayout.LEFT)
                                                    recentlist=list(id: 'type', items: recentNodesVisited)
                                                }
                                                panel(alignmentX: 0f) {
                                                    flowLayout(alignment: FlowLayout.RIGHT)
                                                    button(action: action(name: 'Locate', defaultButton: true,
                                                            closure: {
                                                                // locate the node selected in the table and go to it in the map
                                                                // but do not load it into Map Insights array
                                                                vars.ok=true
                                                                c.find{
                                                                    it.text == vars.type.selectedValue
                                                                }.each {
                                                                    selectNodeByTitle(it.text)
                                                                }
//                                                                                recentDialog.dispose()
                                                            }))
                                                    button(action: action(name: 'Go To', defaultButton: true,
                                                            closure: {
                                                                // locate the node selected in the table and go to it in the map
                                                                // and load it into Map Insights array as the main node for Map Insight
                                                                vars.ok=true
                                                                c.find{
                                                                    it.text == vars.type.selectedValue
                                                                }.each {
                                                                    selectNodeByTitle(it.text)
                                                                    newNode=c.selected
                                                                    loadNodeData(newNode)
                                                                    updateUI(newNode)
                                                                }
                                                                recentDialog.dispose()
                                                                recenthistoryDisplayed=false
                                                            }))
                                                    button(action: action(name: 'Close', closure: {
                                                        recenthistoryDisplayed=false
                                                        recentDialog.dispose()
                                                    }))
                                                }
                                            }
                                        }
                                    }
                            )
                        }
                    }
                    menu(text:'Help') {
                        menuItem() {
                            action(name:'About', closure:{
                                def pane = swing.optionPane(message: "<html>$scriptVersion<p><small>Author: Allan Davies><br><i>ADXSoft</i><br><br><br>https://github.com/adxsoft/MapInsight</small></html>")
                                def dialog = pane.createDialog(frame, scriptVersion)
                                dialog.show()
                            })}
                        menuItem() {
                            action(name:'Installation', closure:{
                                def pane = swing.optionPane(message: installation)
                                def dialog = pane.createDialog(frame, "Installing in Freeplane")
                                dialog.show()
                            })}
                        menuItem() {
                            action(name:'Using Map Insight', closure:{
                                def pane = swing.optionPane(message: helptext1)
                                def dialog = pane.createDialog(frame, "using Map Insight")
                                dialog.show()
                            })}
                        menuItem() {
                            action(name:'Finding Connections', closure:{
                                def pane = swing.optionPane(message: helptext2)
                                def dialog = pane.createDialog(frame, "Finding Connections")
                                dialog.show()
                            })}
                        menuItem() {
                            action(name:'Managing Connectors', closure:{
                                def pane = swing.optionPane(message: helptext3)
                                def dialog = pane.createDialog(frame, "Managing Connectors")
                                dialog.show()
                            })}
                        menuItem() {
                            action(name:'Version History', closure:{
                                def pane = swing.optionPane(message: versionhistory)
                                def dialog = pane.createDialog(frame, "Version History")
                                dialog.show()
                            })}
                    }
                }
            }

            // main central container has five areas arranged vertically
            // that show the relationships of the selected node

            // 1st area is the selected node's parent node in a table
            // 2nd area is any incomming connectors to the selected node
            // 3rd area is the selected node as a white button
            // 4th are is any outgoing connectors from the selected node
            // 5th area is the children of the selected node in a scrollable table

            centralContainer=scrollPane(background: tablebg) {
                vbox {
                    // Parent node details in table (note using customised table renderer MainTableCellRenderer)
                    vbox {
                        parentTable = table(background: tablebg, showGrid: false, gridColor: Color.GRAY) {
                            editing: true
                            tableModel(list: nodes_data.findAll { it.type == "parent" }) {
                                propertyColumn(header: 'Type', propertyName: 'type', editable: false, cellRenderer: new MainTableCellRenderer())
                                propertyColumn(header: 'Node', propertyName: 'nodetext', editable: false, cellRenderer: new MainTableCellRenderer())
                                propertyColumn(header: 'Note', propertyName: 'notetext', editable: false, cellRenderer: new MainTableCellRenderer())
                                propertyColumn( header: 'Details', propertyName: 'details', editable: false, cellRenderer: new MainTableCellRenderer())
                                propertyColumn(header: 'Label', propertyName: 'label', editable: false, cellRenderer: new MainTableCellRenderer())
                            }
                        }
                    }

                    // markers to denote parent relationship to selected node below
                    parent_label = panel(background: tablebg) {label(text: "v",opaque: true,background: tablebg)}
                    // Connectors In details in table (note using customised table renderer MainTableCellRenderer)
                    vbox {
                        hbox {
                            connectorsInTable = table(background: tablebg, showGrid: false, gridColor: Color.GRAY) {
                                editing: true
                                model = tableModel(list: nodes_data.findAll { it.type == "conn-IN" }) {
                                    propertyColumn(header: 'Type', propertyName: 'type', editable: false, cellRenderer: new MainTableCellRenderer())
                                    propertyColumn(header: 'Node', propertyName: 'nodetext', editable: false, cellRenderer: new MainTableCellRenderer())
                                    propertyColumn(header: 'Note', propertyName: 'notetext', editable: false, cellRenderer: new MainTableCellRenderer())
                                    propertyColumn(header: 'Details', propertyName: 'details', editable: false, cellRenderer: new MainTableCellRenderer())
                                    propertyColumn(header: 'Label', propertyName: 'label', editable: false, cellRenderer: new MainTableCellRenderer())
                                }
                            }
                        }
                    }

                    // markers to denote parent relationship to selected node below
                    conn_in_label = panel(background: tablebg) {label(text: "v",opaque: true,background: tablebg)}

                    // currently selected node area
                    vbox {
                        panel(background: tablebg) {
                            flowLayout()
                            hbox {
                                selected_node_button = button(
                                        background: Color.WHITE,
                                        margin: new Insets(10, 10, 10, 10),
                                        contentAreaFilled: false,
                                        opaque: true
                                ) {
                                    action(name: formatNodeTextForCell(newNode.text)) {
                                        selectNodeByID(newNode.nodeID)
                                    }
                                }
                            }

                        }
                    }

                    // markers to denote child relationship to selected node above
                    conn_out_label = panel(background: tablebg) {label(text: "v",opaque: true,background: tablebg)}

                    vbox {
                        hbox {
                            connectorsOutTable = table(background: tablebg, showGrid: false, gridColor: Color.GRAY) {
                                editing: true
                                model = tableModel(list: nodes_data.findAll { it.type == "conn-OUT" }) {
                                    propertyColumn(header: 'Type', propertyName: 'type', editable: false, cellRenderer: new MainTableCellRenderer())
                                    propertyColumn(header: 'Node', propertyName: 'nodetext', editable: false, cellRenderer: new MainTableCellRenderer())
                                    propertyColumn(header: 'Note', propertyName: 'notetext', editable: false, cellRenderer: new MainTableCellRenderer())
                                    propertyColumn(header: 'Details', propertyName: 'details', editable: false, cellRenderer: new MainTableCellRenderer())
                                    propertyColumn(header: 'Label', propertyName: 'label', editable: false, cellRenderer: new MainTableCellRenderer())
                                }
                            }
                        }
                    }

                    children_label = panel(background: tablebg) {label(text: "v",opaque: true,background: tablebg)}

                    // Children node details in table (note using customised table renderer MainTableCellRenderer)
                    vbox {
                        childrenTable = table(background: tablebg, showGrid: false, gridColor: Color.GRAY) {
                            editing: true
                            model = tableModel(list: nodes_data.findAll { it.type == "child" }) {
                                propertyColumn(header: 'Type', propertyName: 'type', editable: false, cellRenderer: new MainTableCellRenderer())
                                propertyColumn(header: 'Node', propertyName: 'nodetext', editable: false, cellRenderer: new MainTableCellRenderer())
                                propertyColumn(header: 'Note', propertyName: 'notetext', editable: false, cellRenderer: new MainTableCellRenderer())
                                propertyColumn(header: 'Details', propertyName: 'details', editable: false, cellRenderer: new MainTableCellRenderer())
                                propertyColumn(header: 'Label', propertyName: 'label', editable: false, cellRenderer: new MainTableCellRenderer())
                            }
                        }
                    }
//                    label(text: '  ') // spacer
                } // end vbox
            }
        }
    }
}

// Display the Main User Interface
setColumnsForDisplay()
setConnectorLabelsForDisplay()
setButtonsForDisplay()
frame.pack()
frame.show()
mapInsightDisplayed=true

connection_candidates_nodes_data = []
connection_candidates_nodes_saved_objects = []


//-----------------------
// == UI FUNCTION: == Connector Candidates
// Shows options for finding connector candidates
// Candidates are selected based on words derived from core text
// and/or notes text
// and/or details text
// and/or specific user entered words
def connectoroptions() {
    useSourceNodeCoreText=true
    useSourceNodeNoteText=false
    useSourceNodeDetailsText=false
    useWords=false
    useWordsText=""
    searchTargetNodeNoteText=false
    searchTargetNodeDetailsText=false

    connectoroptionsdialog = swing.dialog(
            title: "Connector Candidate Options",
            defaultCloseOperation: JFrame.DISPOSE_ON_CLOSE,
            alwaysOnTop: true,
            modal: false,
            location: [mypanel.getAt('width')+5,0]
    )
    def panel = swing.panel{
        vbox {
            vbox{
                checkBox(id: 'cb4',text:"Use the words below",selected: useWords,
                        actionPerformed: {
                            useWords=cb4.selected
                            cb1.selected=false
                            cba.selected=true
                            cbb.selected=true
                            cbc.selected=true
                            searchTargetNodeCoreText=true
                            searchTargetNodeNoteText=true
                            searchTargetNodeDetailsText=true
                            input.requestFocus()
                        })
                label(text: """<html>
                                <body>
                                    eg word1,word2,word3 or<br>
                                    &nbsp;&nbsp; a single phrase in double quotes eg "united kingdom"<br>
                                    &nbsp;&nbsp; a regular expression in forward slashes eg "/^M*/
                                </body>
                                </html>""")
                input = textField(columns:20)
                separator()
                label(text: 'For the current node')
                checkBox(id: 'cb1',text:"Use words from CORE TEXT",selected: useSourceNodeCoreText,actionPerformed: {useSourceNodeCoreText=cb1.selected})
                checkBox(id: 'cb2',text:"Use words from NOTE TEXT",selected: useSourceNodeNoteText,actionPerformed: {useSourceNodeNoteText=cb2.selected})
                checkBox(id: 'cb3',text:"Use words from DETAILS TEXT",selected: useSourceNodeDetailsText,actionPerformed: {useSourceNodeDetailsText=cb3.selected})
                separator()
                label(text: 'For the candidate node')
                checkBox(id: 'cba',text:"Search words in CORE TEXT",selected: true,actionPerformed: {searchTargetNodeCoreText=true})
                checkBox(id: 'cbb',text:"Search words in NOTE TEXT",selected: searchTargetNodeNoteText,actionPerformed: {searchTargetNodeNoteText=cbb.selected})
                checkBox(id: 'cbc',text:"Search words in DETAILS TEXT",selected: searchTargetNodeDetailsText,actionPerformed: {searchTargetNodeDetailsText=cbc.selected})
            }
            hbox{
                button(action: action(name: 'Find Candidates', closure: {
                    if (input.text!="") {
                        useWords=true
                        useWordsText=input.text
                    }
                    connectoroptionsdialog.dispose()
                    connectorUI(newNode)
                }))
                button(action: action(name: 'Cancel', closure: {
                    connectoroptionsdialog.dispose()
                }))
            }
        }
    }
    connectoroptionsdialog.getContentPane().add(panel)
    connectoroptionsdialog.pack()
    connectoroptionsdialog.show()
}

//-----------------------
// == UI FUNCTION: == Connector Candidates User Interface
// ============= Search for possible connections to the selected node based on   =============
// ============= 'proper' words in the selected nodes core text                  =============
def connectorUI(newNode) {

    // clear the working array that will contain any possible candidate nodes
    connection_candidates_nodes_data = []

    // get the current nodes ID
    def currentNodeID=newNode.nodeID

    // get 'proper' words (ie not stopwords) from the current nodes core text

    searchwords=""

    if (useWords) {
        searchwords=useWordsText.replace(',',' ')
    }

    if (useSourceNodeCoreText) {
        searchwords+=" "+removeHtmlTags(newNode.text)
    }

    if (useSourceNodeNoteText) {
        searchwords+=" "+removeHtmlTags(newNode.noteText)
    }

    if (useSourceNodeDetailsText) {
        searchwords+=" "+removeHtmlTags(newNode.detailsText)
    }

    searchwords=searchwords.replace("empty","")

    currentnodewords=[]

    def usePhrase=false
    def useRegex=false

    if (useWordsText.startsWith('"') && useWordsText.endsWith('"')) {
        // check if user wants to search for a specific phrase ie string surrounded with double quotes
        usePhrase=true
        currentnodewords.add(useWordsText.substring(1,useWordsText.length()-1)) // if searching for a phrase use the whole current node text(s)
    } else if (useWordsText.startsWith('/') && useWordsText.endsWith('/')) {
        // check if user wants to search for a specific phrase ie string surrounded with double quotes
        useRegex=true
        currentnodewords.add(useWordsText.substring(1,useWordsText.length()-1)) // if searching for a phrase use the whole current node text(s)
    }

    else {
        // otherwise use individual words in current node text(s)
        words=searchwords.split(" ")
        words.each {
            if (it!="") {
                cleanword=cleanupWord(it)
                if (!cleanword.equals("")) {
                    currentnodewords.add(cleanword)
                }
            }
        }
        currentnodewords=currentnodewords.unique()
        usePhrase=false
        useRegex=false
    }


    // English noise words to be ignored when looking for possible connection candidates
    def stopWords = ["a", "about", "above", "above", "across", "after", "afterwards", "again", "against", "all", "almost", "alone", "along",
                     "already", "also", "although", "always", "am", "among", "amongst", "amoungst", "amount", "an", "and", "another", "any",
                     "anyhow", "anyone", "anything", "anyway", "anywhere", "are", "around", "as", "at", "back", "be", "became", "because",
                     "become", "becomes", "becoming", "been", "before", "beforehand", "behind", "being", "below", "beside", "besides",
                     "between", "beyond", "bill", "both", "bottom", "but", "by", "call", "can", "cannot", "cant", "co", "con", "could",
                     "couldnt", "cry", "de", "describe", "detail", "do", "done", "down", "due", "during", "each", "eg", "eight", "either",
                     "eleven", "else", "elsewhere", "empty", "enough", "etc", "even", "ever", "every", "everyone", "everything",
                     "everywhere", "except", "few", "fifteen", "fify", "fill", "find", "fire", "first", "five", "for", "former",
                     "formerly", "forty", "found", "four", "from", "front", "full", "further", "get", "give", "go", "had", "has",
                     "hasnt", "have", "he", "hence", "her", "here", "hereafter", "hereby", "herein", "hereupon", "hers", "herself",
                     "him", "himself", "his", "how", "however", "hundred", "i","ie", "if", "im", "in", "inc", "indeed", "interest", "into",
                     "is", "it", "its", "itself", "ive", "keep", "last", "latter", "latterly", "least", "less", "ltd", "made", "many",
                     "may", "me", "meanwhile", "might", "mill", "mine", "more", "moreover", "most", "mostly", "move", "much", "must",
                     "my", "myself", "name", "namely", "neither", "never", "nevertheless", "next", "nine", "no", "nobody", "none", "noone",
                     "nor", "not", "nothing", "now", "nowhere", "of", "off", "often", "on", "once", "one", "only", "onto", "or", "other",
                     "others", "otherwise", "our", "ours", "ourselves", "out", "over", "own", "part", "per", "perhaps", "please", "put",
                     "rather", "re", "same", "see", "seem", "seemed", "seeming", "seems", "serious", "several", "she", "should", "show",
                     "side", "since", "sincere", "six", "sixty", "so", "some", "somehow", "someone", "something", "sometime", "sometimes",
                     "somewhere", "still", "such", "system", "take", "ten", "than", "that", "the", "their", "them", "themselves", "then",
                     "thence", "there", "thereafter", "thereby", "therefore", "therein", "thereupon", "these", "they", "theyve","thickv", "thin",
                     "third", "this", "those", "though", "three", "through", "throughout", "thru", "thus", "to", "together", "too", "top",
                     "toward", "towards", "twelve", "twenty", "two", "un", "under", "until", "up", "upon", "us", "very", "via", "was", "we","weve",
                     "well", "were", "what", "whatever", "when", "whence", "whenever", "where", "whereafter", "whereas", "whereby",
                     "wherein", "whereupon", "wherever", "whether", "which", "while", "whither", "who", "whoever", "whole", "whom", "whose",
                     "why", "will", "with", "within", "without", "would", "yet", "you", "your", "yours", "yourself", "yourselves","youve",
                     "the","=","?","-"]

    // search through all nodes
    c.findAllDepthFirst().each {
        candidatenode=it

        // only search if candidate node is not the current node
        if (currentNodeID != candidatenode.nodeID) {

            // strip any html tags in core  text
            candidatetext=removeHtmlTags(candidatenode.text)

            // strip any html tags in note text
            if (searchTargetNodeNoteText) {
                candidatetext+=" "+removeHtmlTags(candidatenode.noteText)
            }

            // strip any html tags in details text
            if (searchTargetNodeDetailsText) {
                candidatetext+=" "+removeHtmlTags(candidatenode.detailsText)
            }

            candidatenodewords=[]
            if (usePhrase || useRegex) {
                // if only searching for a Phrase or Regex strip lead and trail double quote
                // and put into candidate node words array
                candidatenodewords.add(candidatetext)
            } else {
                // load proper words into candidate node words array
                words=candidatetext.split(" ")
                words.each {
                    cleanword=cleanupWord(it)
                    if (!cleanword.equals("")) {
                        candidatenodewords.add(cleanword)
                    }
                }
                candidatenodewords=candidatenodewords.unique()
            }


            // process each proper word in the selected nodes text
            currentnodewords.each {

                currentnodeword=it

                if (currentnodeword != "") {

                    // check if word is a proper word ie NOT a noise (stop) word eg the, of, and etcs
                    if (!stopWords.contains(currentnodeword)) {

                        def found=false

                        if (usePhrase) {
                            // if searching a a phrase look for phrase in candidate node text(s)
                            if (candidatetext.contains(currentnodeword)) {
                                found=true
                            }
                        } else if (useRegex) {
                            // if searching by regular expression look for pattern anywhere in the candidate text
                            if (candidatetext =~ currentnodeword) {
                                found=true
                            }
                        }

                        else {
                            // have proper word, check if it is in the candidate nodes individual words
                            if (candidatenodewords.contains(currentnodeword)) {
                                found = true
                            }
                        }
                        if (found) {

                            // make sure we haven't already grabbed this candidate node
//                            if (nodeIDNotInCurrentTables(candidatenode.nodeID)) {

                            // get parents node text

                            if (candidatenode.parent != null) {
                                parentnodetext = candidatenode.parent.text
                            } else {
                                parentnodetext='root'
                            }

                            // add the candidate node to the working array
                            connection_candidates_nodes_data.add([id: candidatenode.nodeID,
                                                                  type: 'conn?',
                                                                  nodetext: formatNodeTextForCell(candidatenode.text),
                                                                  label: '',
                                                                  notetext: candidatenode.noteText,
                                                                  details: candidatenode.detailsText,
                                                                  properword: currentnodeword.toString().toUpperCase(),
                                                                  parent: parentnodetext])
//                            }
                        }
                    }
                }
            }
        }
    }

    // sort connection_candidates_nodes_data in word,node text order
    if (!usePhrase && !useRegex) {
        connection_candidates_nodes_data.sort { it.properword + it.nodetext }
    }

    // define the connector candidates UI
    // which is a list of candidate nodes
    connectorcandidatesframe = swing.dialog(
            title: "Connection Candidates",
            defaultCloseOperation: JFrame.DISPOSE_ON_CLOSE,
            alwaysOnTop: true,
            modal: false,
            location: [mypanel.getAt('width')+5,0]
    ) {

        // connector panel
        connectorpanel = panel(background: Color.WHITE) {
            gridLayout()
            vbox {
                connectorContainer = vbox {

                    // Possible connection candidate node details in table (note using customised table renderer ConnectorCandidateTableCellRenderer)
                    vbox {
                        panel() {
                            label(text: "Possible Connections", foreground: Color.GRAY)
                        }
                        panel() {
                            borderLayout()
                            vbox {
                                scrollPane {
                                    connectionCandidatesTable = table(background: Color.WHITE, showGrid: false, gridColor: Color.GRAY, autoCreateRowSorter: true) {
                                        editing: true;
                                        model = tableModel(list: connection_candidates_nodes_data) {
//                                            propertyColumn(header: 'ID', propertyName: 'id', editable: false, cellRenderer: new ConnectorCandidateTableCellRenderer(), minWidth: 50);
                                            propertyColumn(header: 'Word', propertyName: 'properword', editable: false, cellRenderer: new ConnectorCandidateTableCellRenderer(), minWidth: 50);
                                            propertyColumn(header: 'Type', propertyName: 'type', editable: false, cellRenderer: new ConnectorCandidateTableCellRenderer());
                                            propertyColumn(header: 'Node', propertyName: 'nodetext', editable: false, cellRenderer: new ConnectorCandidateTableCellRenderer(), minWidth: 250);
                                            propertyColumn(header: 'Note', propertyName: 'notetext', editable: false, cellRenderer: new ConnectorCandidateTableCellRenderer());
                                            propertyColumn(header: 'Details', propertyName: 'details', editable: false, cellRenderer: new ConnectorCandidateTableCellRenderer());
                                            propertyColumn(header: 'Parent', propertyName: 'parent', editable: false, cellRenderer: new ConnectorCandidateTableCellRenderer());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                vbox {
                    hbox {
                        button(action: action(name: 'Locate', defaultButton: true,
                                closure: {
                                    // locate the selected candidate node and go to it in the map
                                    // but do not load it into Map Walker
                                    if (selectedCandidateNode != null) {
                                        c.find {
                                            it.text == selectedCandidateNode.text
                                        }.each {
                                            selectNodeByTitle(it.text)
                                            updateRecentNodesVisited(it.text)
                                        }
                                    }
                                }))
                        button(action: action(name: 'Go To', defaultButton: true,
                                closure: {
                                    // locate the selected candidate node and go to it in the map
                                    // and also load it into Map Walkers array
                                    if (selectedCandidateNode != null) {
                                        c.find {
                                            it.text == selectedCandidateNode.text
                                        }.each {
                                            selectNodeByTitle(it.text)
                                            newNode = c.selected
                                            loadNodeData(newNode)
                                            updateUI(newNode)
                                            selectedCandidateNode = null
                                            connectionCandidatesDisplayed = false
                                            connectorcandidatesframe.dispose()
                                        }
                                    }
                                }))
                        button(action: action(name: 'Close', closure: {
                            connectionCandidatesDisplayed = false
                            connectorcandidatesframe.dispose()
                        }))
                    }
                    hbox {
                        button(action: action(name: 'Connect ALL', defaultButton: true,
                                closure: {
                                    // Make connectors for ALL the suggested candidates and
                                    // use the linking word as the connector label

                                    // get current node selected
                                    def sourcenode = newNode

                                    // for each suggested candidate add a connector
                                    connection_candidates_nodes_data.findAll().each {
                                        def newconnector = sourcenode.addConnectorTo(getNodeByID(it['id']))
                                        newconnector.setMiddleLabel(it['properword'].toLowerCase())
                                        newconnector.setStartArrow(false)
                                        newconnector.setEndArrow(true)
                                    }
                                }))
                        button(action: action(name: 'Undo "Connect ALL"', defaultButton: true,
                                closure: {
                                    c.undo()
                                }))
                        button(action: action(name: 'Save as New Map', defaultButton: true,
                                closure: {
                                    // export the source node and every possible node to
                                    // a new map with connectors

                                    // get current node selected
                                    def sourcenode = newNode

                                    // get current timestamp
                                    def date = new Date()
                                    sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss")

                                    // for each candidate get the target node to export
                                    def exportnodes=[]

                                    tabledata="<TR><TD><b>Word</b></TD><TD><b>Candidate</b></TD></TR>"

                                    connection_candidates_nodes_data.findAll().each {
                                        exportnode=getNodeByID(it["id"])
                                        if (exportnode!=null) {
                                            tabledata += "<TR><TD>${it["properword"]}</TD><TD>${it["nodetext"]}</TD></TR>"
                                            exportnodes.add([getNodeByID(it["id"]),it])
                                        }
                                    }
                                    htmlstring="""
                                                <H3>Connector Candidates ${sdf.format(date)}</H3>
                                                <TABLE border="1">
                                                    $tabledata
                                                </TABLE>
                                               """
                                    // create a new map
                                    def newMap = c.newMap()

                                    // copy current node to the root node of the new map
                                    copyProperties(newMap.root,sourcenode)



                                    // create a read me node with details of the possible candidates
                                    def readmenode = newMap.root.createChild()
                                    readmenode.text="READ ME"
                                    readmenode.noteText=htmlstring


                                    // export candidate nodes to the new map as children of the root node
                                    exportnodes.each{

                                        // create new node
                                        def newnode = newMap.root.createChild()

                                        // copy details from candidate node
                                        copyProperties(newnode, it[0])

                                        // add connector from cnadidate node back to root node with
                                        // label that 'made the connection'
                                        def newconnector=newnode.addConnectorTo(newMap.root)
                                        newconnector.setMiddleLabel(it[1]["properword"])
                                        newconnector.setStartArrow(false)
                                        newconnector.setEndArrow(true)
                                    }

                                }))
                    }
                }
            }
        }
    }

    // prevent some columns in the underlying table model from displaying in the connector candidates UI
    HideTableColumns(connectionCandidatesTable,['type','notetext','details','label', 'parent'])


    // Mouse listener for the connector candidates UI
    connectionCandidatesTable.addMouseListener(
            new MouseAdapter() {

                // ensure we can reference the global scope
                def globalscope=this.this$0

                // Trick to get Tooltip for cell to stay visible long enough to read
                final int defaultDismissTimeout = ToolTipManager.sharedInstance().getDismissDelay();
                final int dismissDelayMinutes = (int) TimeUnit.MINUTES.toMillis(100); // 10 minutes
                public void mouseEntered(MouseEvent event) {
                    ToolTipManager.sharedInstance().setDismissDelay(dismissDelayMinutes);
                }
                public void mouseExited(MouseEvent event) {
                    ToolTipManager.sharedInstance().setDismissDelay(defaultDismissTimeout);
                }

                // Take action on mouse click
                public void mousePressed(MouseEvent event) {

                    if ((event.getModifiers() & InputEvent.BUTTON1_MASK) != 0) {

                        // ---- Left Click

                        // get node that has been selected as a candidate
                        def selectedcandidatenodedata=globalscope.connection_candidates_nodes_data[event.getSource().getSelectedRow()]
                        globalscope.selectedCandidateNode=findNodeByID(selectedcandidatenodedata["id"])

                        // on double click load the node into Map Insight as the main node in focus
                        if (event.getClickCount()==2) {
                            displayNewNode(event.component,globalscope.connection_candidates_nodes_data)
                            globalscope.connectorcandidatesframe.dispose()
                        }
                    }else if ((event.getModifiers() & InputEvent.BUTTON3_MASK) != 0) {

                        // ---- Right Click

                        // display dialog to create a connector from the Map Insight focussed node to the candidate node

                        def sourcenode=globalscope.newNode

                        def targetnodedata=globalscope.connection_candidates_nodes_data[event.getSource().getSelectedRow()]
                        def targetnode=getNodeByID(targetnodedata["id"])

                        // Create Connector UI

                        def swing = new SwingBuilder()
                        def middlelabel=""
                        def createconnectordialog = swing.dialog(title:'Create Connector',
                                id:'createConnectorDialog',
                                minimumSize: [300,50],
                                modal:true,
                                alwaysOnTop: true,
                                owner:globalscope.connectorcandidatesframe,
                                defaultCloseOperation:JFrame.DO_NOTHING_ON_CLOSE,
                                //                    Using DO_NOTHING_ON_CLOSE so the Close button has full control
                                //                    and it can ensure only one instance of the dialog appears
                                pack:true,
                                show:true) {
                            panel() {
                                boxLayout(axis: BoxLayout.Y_AXIS)
                                vbox {
                                    panel() {
                                        flowLayout()
                                        vbox {
                                            //                                        gridLayout(columns: 1, rows: 8)
                                            label(text: 'Create connector from', horizontalAlignment: JLabel.CENTER)
                                            label(text: "$sourcenode.text", horizontalAlignment: JLabel.CENTER, foreground: Color.BLUE)
                                            label(text: "to", horizontalAlignment: JLabel.CENTER)
                                            label(text: "$targetnode.text", horizontalAlignment: JLabel.CENTER, foreground: Color.BLUE)
                                            separator()
                                            label(text: "Enter new connector middle label ", horizontalAlignment: JLabel.CENTER)
                                            scrollPane() {
                                                def input = textArea(columns: 20, rows: 3, text: targetnodedata['properword'].toLowerCase())
                                                input.addFocusListener(
                                                        [focusGained: {},
                                                         focusLost  : {
                                                             middlelabel = input.text
                                                         }] as FocusListener)
                                            }
                                        }
                                    }
                                    hbox {
                                        button(action: action(name: 'Add Connector', closure: {
                                            def newconnector = sourcenode.addConnectorTo(targetnode)
                                            newconnector.setMiddleLabel(middlelabel.toLowerCase())
//                                            newconnector.setStartArrow(false)
                                            newconnector.setEndArrow(true)
                                            dispose()
                                        }))
                                        button(action: action(name: 'Close', closure: {
                                            dispose()
                                        }))
                                    }
                                }
                            }
                        }
                    }

                    // double mouse click

                    else if (event.getClickCount()==2) {
                        // do nothing on double click
                    }
                }
            }
    )

    connectorcandidatesframe.pack()
    connectorcandidatesframe.show()

}

//****************************************************
// UI - class overrides the connector candidate table
// cell renderer in the Connector Candidates View
//****************************************************

class ConnectorCandidateTableCellRenderer extends JLabel implements TableCellRenderer {
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int rowIndex, int vColIndex) {

        setText(removeHtmlTags(value.toString()))

        // center the text in the cell
        setHorizontalAlignment(SwingConstants.LEFT)

        // color the cell based on whether the refered node is
        // a parent of the selected node
        // a child of the selected node
        // a connection into or out of the selected node

        if (isSelected)
        {
            setBackground(Color.GRAY);
            setForeground(Color.BLUE);
        }
        else
        {
            setBackground(table.getBackground());
            setForeground(table.getForeground());
        }

        if (vColIndex==0) {
            setForeground(Color.RED)
        }

        // Get the current model row from the table (this works even when the table columns are sorted)
        def modelRow = table.convertRowIndexToModel(rowIndex)

        // Set up tooltip for cell which shows the note and detail texts related to the node the cell refers to
        def searchword=table.model.getValueAt(modelRow,0)
        String nodetype=removeHtmlTags(value.toString())+"<HR>Connected via word -> "+searchword+'<HR>'
        String notetext=table.model.getValueAt(modelRow,3)
        if (notetext==null) notetext="empty"
        String detailstext=table.model.getValueAt(modelRow,4)
        String parentnodetext=table.model.getValueAt(modelRow,5)
        if (detailstext==null) detailstext="empty"

        // format tooltip text for display
        def tooltiptext=formatForSwingDisplay(nodetype,value,notetext,detailstext).replace('</body></HTML>','')+'<hr><i>Parent is '+ parentnodetext+'</i></body></HTML>'
        // and highlight the searched word in red (lower case, upper case and first letter capitalised
        if (searchword.trim().length()>2) {
            tooltiptext=tooltiptext.replace(searchword.toLowerCase(),'<font color="RED">'+searchword.toLowerCase()+'</font>')
            tooltiptext=tooltiptext.replace(searchword.toUpperCase(),'<font color="RED">'+searchword.toUpperCase()+'</font>')
            tooltiptext=tooltiptext.replace(searchword[0].toUpperCase() + searchword[1..-1].toLowerCase()
                    ,'<font color="RED">'+searchword[0].toUpperCase() + searchword[1..-1].toLowerCase()+'</font>')
        }
        setToolTipText((String) tooltiptext)


        return this
    }
}

//*************************************************
// UI - Mouse Listeners for Map Insight Main Window
//*************************************************

connectorsInTable.addMouseListener(
        new MouseAdapter() {

            // global scope
            def globalscope=this.this$0

            // Trick to get Tooltip for cell to stay visible long enough to read
            final int defaultDismissTimeout = ToolTipManager.sharedInstance().getDismissDelay();
            final int dismissDelayMinutes = (int) TimeUnit.MINUTES.toMillis(100); // 10 minutes
            public void mouseEntered(MouseEvent event) {
                ToolTipManager.sharedInstance().setDismissDelay(dismissDelayMinutes);
            }
            public void mouseExited(MouseEvent event) {
                ToolTipManager.sharedInstance().setDismissDelay(defaultDismissTimeout);
            }

            // Take action on mouse click
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount()==2) {
                    displayNewNode(event.component,globalscope.nodes_data.findAll{it.type=="conn-IN"})
                    // note. the reference to this.this$0 is the only way I could find to get at the
                    // working arrays in the script level variables. There is probably a much
                    // easier way to do this.
                }
            }
        }
)

parentTable.addMouseListener(
        new MouseAdapter() {

            // global scope
            def globalscope=this.this$0

            // Trick to get Tooltip for cell to stay visible long enough to read
            final int defaultDismissTimeout = ToolTipManager.sharedInstance().getDismissDelay();
            final int dismissDelayMinutes = (int) TimeUnit.MINUTES.toMillis(100); // 10 minutes
            public void mouseEntered(MouseEvent event) {
                ToolTipManager.sharedInstance().setDismissDelay(dismissDelayMinutes);
            }
            public void mouseExited(MouseEvent event) {
                ToolTipManager.sharedInstance().setDismissDelay(defaultDismissTimeout);
            }

            // Take action on mouse click
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount()==2) {
                    displayNewNode(event.component,globalscope.nodes_data.findAll{it.type=="parent"})
                }
            }
        }
)

childrenTable.addMouseListener(
        new MouseAdapter() {

            // global scope
            def globalscope=this.this$0

            // Trick to get Tooltip for cell to stay visible long enough to read
            final int defaultDismissTimeout = ToolTipManager.sharedInstance().getDismissDelay();
            final int dismissDelayMinutes = (int) TimeUnit.MINUTES.toMillis(100); // 10 minutes
            public void mouseEntered(MouseEvent event) {
                ToolTipManager.sharedInstance().setDismissDelay(dismissDelayMinutes);
            }
            public void mouseExited(MouseEvent event) {
                ToolTipManager.sharedInstance().setDismissDelay(defaultDismissTimeout);
            }

            // Take action on mouse click
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    displayNewNode(event.component, globalscope.nodes_data.findAll{it.type=="child"})
                }
            }
        }
)

connectorsOutTable.addMouseListener(
        new MouseAdapter() {

            // global scope
            def globalscope=this.this$0

            // Trick to get Tooltip for cell to stay visible long enough to read
            final int defaultDismissTimeout = ToolTipManager.sharedInstance().getDismissDelay();
            final int dismissDelayMinutes = (int) TimeUnit.MINUTES.toMillis(100); // 10 minutes
            public void mouseEntered(MouseEvent event) {
                ToolTipManager.sharedInstance().setDismissDelay(dismissDelayMinutes);
            }
            public void mouseExited(MouseEvent event) {
                ToolTipManager.sharedInstance().setDismissDelay(defaultDismissTimeout);
            }

            // Take action on mouse click
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount()==2) {
                    displayNewNode(event.component,globalscope.nodes_data.findAll{it.type=="conn-OUT"})
                }
            }
        }
)

//*********************************************
//         UI - Connector Manager Window
//*********************************************
def connectorManagerUI() {
    // ============= Manage all connectors in the map   =============

    def selectedconnectorobject=null

    // clear working array
    connectors_data = []

    // search through all nodes for connectors out
    node.map.root.findAll().each {
        def theNodeID = it.nodeID

        // retrieve each connector OUT from the node
        it.connectorsOut.each {
            it.setColor(Color.GRAY)
            def middlelabel = it.middleLabel
            sourceNode = getNodeByID(theNodeID)
            targetNode = getNodeByID(it.delegate.target.id)
            connectors_data.add([type       : 'conn-OUT',
                                 sourceID: sourceNode.id,
                                 sourcenodetext: formatNodeTextForCell(sourceNode.text),
                                 middlelabel: middlelabel,
                                 targetnodetext: formatNodeTextForCell(targetNode.text)])
        }
    }

    connectorsframe = swing.dialog(
            title: "Connectors",
            defaultCloseOperation: JFrame.DISPOSE_ON_CLOSE,
            alwaysOnTop: true,
            modal: false,
            location: [mypanel.getAt('width') + 5, 0]
    ) {

        // connector panel
        connectorspanel = panel(background: Color.WHITE) {
            gridLayout()
            vbox {

                connectorsContainer = vbox {
                    vbox {
                        panel() {
                            label(text: "Manage All Connectors", foreground: Color.GRAY)
                        }
                        panel() {
                            borderLayout()
                            vbox {
                                scrollPane {
                                    connectorsTable = table(background: Color.WHITE, showGrid: false, gridColor: Color.GRAY,autoCreateRowSorter: true) {
                                        editing: true;
                                        model = tableModel(list: connectors_data) {
                                            propertyColumn(header: 'Source Node', propertyName: 'sourcenodetext', editable: false, cellRenderer: new ConnectorsTableCellRenderer(), minWidth: 100);
                                            propertyColumn(header: 'Middle Label', propertyName: 'middlelabel', editable: false, cellRenderer: new ConnectorsTableCellRenderer(), minWidth: 100);
                                            propertyColumn(header: 'Target Node', propertyName: 'targetnodetext', editable: false, cellRenderer: new ConnectorsTableCellRenderer(), minWidth: 100);

                                        }
                                    }
                                }
                            }
                        }

                        panel() {
                            panel(alignmentX: 0f) {
                                flowLayout(alignment: FlowLayout.RIGHT)
                                vbox {
                                    hbox {
                                        editconnectorbutton = button(action: action(name: 'Edit Label', defaultButton: true, enabled: false,
                                                closure: {
                                                    def sourcenode = getNodeByID(selectedconnectorobject.delegate.source.id)
                                                    def targetnode = getNodeByID(selectedconnectorobject.delegate.targetID)


                                                    def sourcenodetext = sourcenode.text
                                                    def targetnodetext = targetnode.text
                                                    def middlelabel = selectedconnectorobject.delegate.middleLabel

                                                    editlabeldialog = swing.dialog(
                                                            title: "Change Connector Label",
                                                            defaultCloseOperation: JFrame.DISPOSE_ON_CLOSE,
                                                            alwaysOnTop: true,
                                                            modal: false,
                                                            location: [mypanel.getAt('width') + connectorspanel.getAt('width') + 5, 0]
                                                    )
                                                    def panel = swing.panel {
                                                        vbox {
                                                            label(text: "Enter new connector label")
                                                            input = textField(columns: 20, text: middlelabel)
                                                        }
                                                        hbox {
                                                            button(action: action(name: 'OK', closure: {

                                                                if (input.text != "") {
                                                                    selectedconnectorobject.delegate.middleLabel = input.text
                                                                    selectedconnectorobject.setColor(Color.GRAY)
                                                                }

                                                                editlabeldialog.dispose()
                                                                connectorsframe.dispose()
                                                            }))
                                                            button(action: action(name: 'Cancel', closure: {
                                                                editlabeldialog.dispose()
                                                            }))
                                                        }
                                                    }
                                                    editlabeldialog.getContentPane().add(panel)
                                                    editlabeldialog.pack()
                                                    editlabeldialog.show()
                                                }))
                                        removeconnectorbutton = button(action: action(name: 'Remove Connector', defaultButton: true, enabled: false,
                                                closure: {
                                                    // Remove connector UI dialog


                                                    def sourcenode = getNodeByID(selectedconnectorobject.delegate.source.id)
                                                    def targetnode = getNodeByID(selectedconnectorobject.delegate.targetID)


                                                    def sourcenodetext = sourcenode.text
                                                    def targetnodetext = targetnode.text
                                                    def middlelabel = selectedconnectorobject.delegate.middleLabel

                                                    //                                            def swing = new SwingBuilder()
                                                    removeconnectordialog = swing.dialog(title: 'Remove Connector',
                                                            id: 'removeConnectorDialog',
                                                            minimumSize: [100, 50],
                                                            modal: false,
                                                            alwaysOnTop: true,
                                                            defaultCloseOperation: JFrame.DO_NOTHING_ON_CLOSE,
                                                            //                    Using DO_NOTHING_ON_CLOSE so the Close button has full control
                                                            //                    and it can ensure only one instance of the dialog appears
                                                            pack: true,
                                                            show: true) {
                                                        panel() {
                                                            boxLayout(axis: BoxLayout.Y_AXIS)
                                                            panel() {
                                                                gridLayout(columns: 1, rows: 8)
                                                                label(text: 'Remove this connector?', horizontalAlignment: JLabel.CENTER)
                                                                label(text: "")
                                                                label(text: "from: $sourcenodetext", horizontalAlignment: JLabel.CENTER, foreground: Color.BLUE)
                                                                label(text: "")
                                                                label(text: "label: $middlelabel", horizontalAlignment: JLabel.CENTER)
                                                                label(text: "")
                                                                label(text: "to: $targetnodetext", horizontalAlignment: JLabel.CENTER, foreground: Color.BLUE)
                                                            }
                                                            panel() {
                                                                hbox {
                                                                    button(action: action(name: 'Remove Connector', closure: {
                                                                        //locate connectorOut within the node
                                                                        sourcenode.connectorsOut.each {
                                                                            def targetNode = getNodeByID(it.delegate.target.id)
                                                                            if (targetNode.text.equals(targetnodetext) &&
                                                                                    sourcenode.text.equals(sourcenodetext) &&
                                                                                    middlelabel.equals(it.delegate.middleLabel)) {
                                                                                sourcenode.removeConnector(it)
                                                                            }
                                                                        }
                                                                        removeconnectordialog.dispose()
                                                                        connectorsframe.dispose()
                                                                    }))
                                                                    button(action: action(name: 'Cancel', closure: {
                                                                        removeconnectordialog.dispose()
                                                                    }))
                                                                }
                                                            }
                                                        }
                                                    }

                                                }))
                                        button(action: action(name: 'Close', closure: {
                                            connectorsDisplayed = false
                                            connectorsframe.dispose()
                                        }))
                                    }
                                    hbox {
                                        button(action: action(name: 'Remove ALL Connectors', defaultButton: true,
                                                closure: {
                                                    removeAllConnectors()
                                                    connectorsDisplayed = false
                                                    connectorsframe.dispose()
                                                }))
                                        button(action: action(name: 'Undo "Remove ALL Connectors"', defaultButton: true,
                                                closure: {
                                                    c.undo()
                                                    connectorsDisplayed = false
                                                    connectorsframe.dispose()
                                                }))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // prevent some columns in the underlying table model from displaying in the connector candidates UI
    HideTableColumns(connectorsTable,['sourceID'])

    // add mouse listener to the connectors manager table
    connectorsTable.addMouseListener(
            new MouseAdapter() {
                // global scope
                def globalscope=this.this$0

                def chosenconnector=null

                // Take action on mouse click
                public void mousePressed(MouseEvent event) {
                    def selectedConnectortablerow=null
                    def sourcenodetext=""
                    def middlelabel=""
                    def targetnodetext=""

                    if ((event.getModifiers() & InputEvent.BUTTON1_MASK) != 0) {

                        // Left Click - select the connector

                        // get the connector data from the selected table entry
                        selectedConnectortablerow=globalscope.connectors_data[event.getSource().getSelectedRow()]
                        sourcenodetext=selectedConnectortablerow["sourcenodetext"]
                        middlelabel=selectedConnectortablerow["middlelabel"]
                        targetnodetext=selectedConnectortablerow["targetnodetext"]

                        // get the source node of the connector
                        def sourceNode = getNodeByID(selectedConnectortablerow['sourceID'])

                        selectedconnectorobject=null

                        //locate connectorOut within the node
                        sourceNode.connectorsOut.each {
                            def targetNode = getNodeByID(it.delegate.target.id)
                            if (targetNode.text.equals(targetnodetext) &&
                                    sourceNode.text.equals(sourcenodetext) &&
                                    middlelabel.equals(it.delegate.middleLabel)) {
                                setAllConnectorsToDefaultColor()

                                // highlight the selected connector in the map to BLUE
                                it.setColor(Color.BLUE)

                                // save the reference to the connector object
                                selectedconnectorobject=it

                                // enable connector buttons
                                globalscope.editconnectorbutton.enabled=true
                                globalscope.removeconnectorbutton.enabled=true
                            }
                        }

                    }

                    // double mouse click

                    else if (event.getClickCount()==2) {
                        // do nothing
                    }
                }
            }
    )

    connectorsframe.pack()
    connectorsframe.show()

}

//**********************************************
// UI class overrides the connector table cell
// renderer in the Connectors View
//**********************************************
class ConnectorsTableCellRenderer extends JLabel implements TableCellRenderer {
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int rowIndex, int vColIndex) {

        // Set up tooltip for cell which shows the note and detail texts related to the node the cell refers to
        setText(removeHtmlTags(value.toString()))

        // align the text in the cell
        setHorizontalAlignment(SwingConstants.LEFT)

        if (isSelected)
        {
            setBackground(Color.GRAY);
            setForeground(Color.BLUE);
        }
        else
        {
            setBackground(table.getBackground());
            setForeground(table.getForeground());
        }

        return this
    }
}


// END OF SCRIPT/**
