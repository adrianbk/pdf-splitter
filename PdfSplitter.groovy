@GrabConfig(systemClassLoader=true) 
@Grab(group='com.lowagie', module='itext', version='2.1.4')

import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter
import groovy.swing.SwingBuilder
import java.awt.BorderLayout as BL
import java.awt.Color
import com.lowagie.text.pdf.*
import com.lowagie.text.Document
import com.lowagie.text.pdf.PdfCopy
import groovy.transform.Field
import groovy.beans.Bindable


//When a variable is typed its not global - use @Field to make it global to the script
@Field int numberOfSplits = 5
@Field File pdfFile = null
@Field List inputFileds = []
@Field swingBuilder = new SwingBuilder()


pdfChooser = new JFileChooser(
    dialogTitle: "Choose an pdf file",
    fileSelectionMode: JFileChooser.FILES_ONLY,
    fileFilter: new FileNameExtensionFilter("PDF Files", "pdf"))

def selectFile( event = null ) {
    int result = pdfChooser.showOpenDialog()
    if( result == JFileChooser.APPROVE_OPTION ) {
        pdfFile = pdfChooser.selectedFile
        fileLabel.text = pdfFile
    }
}

private PdfReader createPdf(){
    PdfReader reader = new PdfReader(pdfFile.toString())
    reader
}

class FileProgress {
    @Bindable def current = 0
}

def splitFiles() {
    if(validateInputs()) {
 /*      PdfReader reader = createPdf()
        int totalPages = reader.numberOfPages
        Document document = new Document(reader.getPageSizeWithRotation(1))
        document.open() */   
        inputFileds.each{
            if(hasInputs(it)){
                Integer  fromPage =  it.fromPageField.text.toInteger()
                Integer  toPage = it.toPageField.text.toInteger()
                String   outFileName =  it.fileNameField.text
                def progressBar = it.progressBar
                def fileProgress = it.fileProgress

      //          PdfCopy copy = new PdfCopy(document, new FileOutputStream("${outFileName}.pdf"))
                progressBar.value = 0
                progressBar.maximum = toPage

                
                
                (fromPage .. toPage).each{ pageNumber->
                    fileProgress.current +=1 

                    println "Attempting Page: $pageNumber. ${Thread.currentThread()}"
                        sleep(2000)
        //            copy.addPage(copy.getImportedPage(reader, pageNumber))
                
                    
                }
            }
        }
    //    document.close()
    
    }  
}

def error(String message) {
    JOptionPane.showMessageDialog mainFrame, message, "Error", JOptionPane.ERROR_MESSAGE
}

def reset() {
    pdfFile = null;
    fileLabel.text = ""
    inputFileds.each{map -> map.findAll { it.value instanceof javax.swing.JTextField }.each { key, value -> 
        value.text = "" 
        value.setBackground(Color.white)
        }
    }
    inputFileds.findAll {f -> f.value instanceof javax.swing.JTextField}.each{println it}
}

def hasInputs(m){
    def fromField = m.fromPageField
    def toField = m.toPageField
    def fileName = m.fileNameField
    fromField.text && toField.text && fileName.text ? true : false
}

Boolean validateInputs() {
    boolean valid = false
    if(null == pdfFile || !pdfFile.exists()){
        error("Invalid PDF file!")
        return false
    }
    inputFileds.each{map -> 
        def fromField = map.fromPageField
        def toField = map.toPageField
        def fileName = map.fileNameField
        def textFieldList = map.values().findAll{it instanceof javax.swing.JTextField }
        if(fromField.text || toField.text || fileName.text) {
            invalidFileds = textFieldList.findAll{ it?.text?.isAllWhitespace()}
            int fromP = fromField.text?.isNumber() ? fromField.text.toInteger() : 0
            int toP = toField.text?.isNumber() ? toField.text.toInteger() : 0 
            println "Size is : " + invalidFileds.size()
            if(toP < fromP || toP == 0 || fromP == 0){
                invalidFileds << toField << fromField
            }
            invalidFileds*.setBackground(new Color(246, 163, 163))
            valid = invalidFileds.size() < 1 ?: false 
            (textFieldList - invalidFileds )*.setBackground(Color.white)
        } else{
             textFieldList.findAll{it instanceof javax.swing.JTextField}*.setBackground(Color.white)
        }
    }
    return valid
}   

def size = [900, 400]
swingBuilder.edt {
	mainFrame = frame(title: 'PDF Splitter', defaultCloseOperation: JFrame.DISPOSE_ON_CLOSE, 
		size: size, minimumSize: size, maximumSize: size, resizable: false, show: true, locationRelativeTo: null) {
    
    borderLayout()

    panel(constraints:BL.NORTH, border: compoundBorder([emptyBorder(10), titledBorder('Select the PDF file to split:')])) {
         label(text:"PDF File: ")
         fileLabel = textField(text: 'Select one..', editable: false, columns: 50)
         button(text:'Select file', actionPerformed: {e -> this.selectFile(e)})
     }

     panel(constraints:BL.CENTER, border: compoundBorder([emptyBorder(10), titledBorder('Specify ouput files:')])) {
        (1..numberOfSplits).each{ i ->
            String mapKey = "inputSet$i"
            Map m = [:]
            FileProgress fp = new FileProgress()
            label(text: "From Page (inclusive): ")
            m."fromPageField" = textField(columns: 5)
            label(text:  "To (inclusive): ")
            m."toPageField" = textField(columns: 5)
            label(text:  "Filename: ")
            m."fileNameField" = textField(columns: 20)
            m."progressBar" = progressBar(value: bind {fp.current} ) 
            m."fileProgress" = fp 
            inputFileds.add(m)        
        }
     }

     panel(constraints:BL.SOUTH, border: compoundBorder([emptyBorder(10), titledBorder('Execute:')])){
        println "${Thread.currentThread()}"
        button(text: "> Reset", actionPerformed: {e -> this.reset()})
        button(text: "> Split", actionPerformed: {e ->  doLater{this.splitFiles()}})
     }
 }
}



