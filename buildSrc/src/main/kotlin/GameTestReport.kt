import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

fun normalizeGameTestReport(report: File) {
    if (!report.isFile) {
        return
    }

    val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(report)
    val root = document.documentElement
    var changed = false

    if (root.tagName == "testsuite" && root.getElementsByTagName("testsuite").length > 0) {
        document.renameNode(root, null, "testsuites")
        changed = true
    }

    val suites = document.getElementsByTagName("testsuite")
    for (i in 0 until suites.length) {
        val suite = suites.item(i) as Element
        if (!suite.hasAttribute("name")) {
            suite.setAttribute("name", "gameTest")
            changed = true
        }
    }

    if (changed) {
        TransformerFactory.newInstance().newTransformer()
            .transform(DOMSource(document), StreamResult(report))
    }
}
