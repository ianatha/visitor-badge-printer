package io.atha.VisitorBadgePrinter

import java.util.{Properties, List, Date}
import java.awt._
import java.awt.image.BufferedImage
import java.awt.print.{Printable, PageFormat, Paper, PrinterJob}
import java.net.URL
import javax.print.attribute.HashPrintRequestAttributeSet
import javax.print.PrintService
import org.fit.cssbox.css.{CSSNorm, DOMAnalyzer}
import org.fit.cssbox.io.{DefaultDocumentSource, DefaultDOMSource}
import org.fit.cssbox.layout.BrowserCanvas
import scala.collection.JavaConversions._
import xml.{Text, XML}
import java.io.{FileInputStream, File, BufferedInputStream}
import org.joda.time.DateTime
import util.matching.Regex

case class Configuration(
  printerName: String = "DYMO LabelWriter 450 Turbo",
  badgeTemplate: String = "$HOME/BadgeTemplate.html",
  remoteDataSource: String = "http://example.com/",
  pollingFrequencyInUSec: Int = 2500
)

case class VisitorBadge(id: Int, visitor: String, host: String, created_at: DateTime)

class BadgeRenderer(val templateURL: URL) {
  val docSource = new DefaultDocumentSource(templateURL)

  val template = {
    val parser = new DefaultDOMSource(docSource)
    parser.parse()
  }

  def render(dimension: Dimension, bean: Object): BufferedImage = {
    val domAnalyzer = new DOMAnalyzer(template, docSource.getURL)

    domAnalyzer.attributesToStyles()
    domAnalyzer.addStyleSheet(null, CSSNorm.stdStyleSheet(), DOMAnalyzer.Origin.AGENT)
    domAnalyzer.addStyleSheet(null, CSSNorm.userStyleSheet(), DOMAnalyzer.Origin.AGENT)
    domAnalyzer.getStyleSheets()

    val nodeList = domAnalyzer.getRoot().getElementsByTagName("*")
    println(nodeList.getLength)
    for (i <- 0 to nodeList.getLength - 1) {
      val node = nodeList.item(i)
      if (node.getNodeType == org.w3c.dom.Node.ELEMENT_NODE) {
        val dataBind = node.getAttributes.getNamedItem("data-bind")
        if (dataBind != null) {
          node.setTextContent(getValueFromBean(bean, dataBind.getNodeValue))
        }
      }
    }

    val contentCanvas = new BrowserCanvas(domAnalyzer.getRoot(), domAnalyzer, docSource.getURL)
    contentCanvas.getConfig().setLoadImages(true)
    contentCanvas.getConfig().setLoadBackgroundImages(true)
    contentCanvas.createLayout(dimension)
    contentCanvas.setAutoSizeUpdate(true)

    contentCanvas.getImage()
  }

  def getValueFromBean(bean: Object, field: String): String = {
    if (bean.isInstanceOf[Map[_,_]]) {
      bean.asInstanceOf[Map[_,_]].get(field).asInstanceOf[String]
    } else {
      val beanField = bean.getClass.getDeclaredField(field)
      beanField.setAccessible(true)
      beanField.get(bean).toString
    }
  }
}

class BadgePrinter(val printerName: String, val renderer: BadgeRenderer) {
  val margin = 0.00f
  val width = 2.125f - margin * 2
  val height = 4.00f - margin * 2

  val printService: PrintService = {
    PrinterJob.lookupPrintServices.find { printService =>
      printService.getName == printerName
    }.getOrElse({
      throw new IllegalArgumentException("Couldn't find printer '%s'".format(printerName))
    })
  }

  def print(badge: VisitorBadge) = {
    val badgeImage = renderer.render(new Dimension(288- 18*2 , 153-5*2), badge)

    val printerJob = PrinterJob.getPrinterJob
    val pageFormat = printerJob.defaultPage()
    val paper = new Paper()
    paper.setSize((width + margin * 2) * 72, (height + margin * 2) * 72)
    paper.setImageableArea(margin * 72, margin * 72, width * 72, height * 72)
    pageFormat.setPaper(paper)
    pageFormat.setOrientation(PageFormat.LANDSCAPE)

    printerJob.setPrintable(new Printable {
      def print(graphics: Graphics, pageFormat: PageFormat, page: Int): Int = {
        if (page != 0) {
          Printable.NO_SUCH_PAGE
        } else {
          val canvas = graphics.asInstanceOf[Graphics2D]
          canvas.translate(pageFormat.getImageableX, pageFormat.getImageableY)
          canvas.setColor(Color.WHITE)
          val renderingHints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)
          renderingHints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
          canvas.setRenderingHints(renderingHints)

          val x_margin = 18
          val y_margin = 5

          canvas.translate(x_margin, y_margin)

          canvas.drawImage(badgeImage,
            0, 0,
            (canvas.getClipBounds().getWidth - x_margin * 2).asInstanceOf[Int], (canvas.getClipBounds().getHeight - y_margin * 2).asInstanceOf[Int],
            null, null)

          Printable.PAGE_EXISTS
        }
      }
    }, pageFormat);
    printerJob.setPrintService(printService)

    val requestConf = new HashPrintRequestAttributeSet()
      //            docAttributeSet.add(new MediaPrintableArea(0, 0, width, height, MediaPrintableArea.INCH));
      //            docAttributeSet.add(MediaSize.findMedia(width, height, MediaSize.INCH));

    printerJob.print(requestConf)
  }
}

class DataSource(val remoteURL: String) {
  var idsIHaveSeen: Seq[Int] = Seq()

  val pattern = "data-person-id=\"(\\d+)\">(.+)\\( (.+) ago \\)".r
  val agonessP = "(?:about )?(\\d+|less than a) ((minute|hour)s?)".r

  def sanitize(in: String): String = in.replaceAll("&nbsp;", " ").replaceAll("&#x27;", "'")

  def poll(): List[VisitorBadge] = {
    val present = scala.io.Source.fromInputStream(new URL(remoteURL).openStream()).getLines().mkString("\n")

    val entries = (pattern.findAllIn(present) map ( _ match {
      case pattern(id, name, agoness) => {
        val now = DateTime.now()
        val when: DateTime = (agonessP.findAllIn(agoness) map ( _ match {
          case agonessP(num, units, unit) => {
            if (unit == "hour") {
              now.minusHours(num.toInt)
            } else if (unit == "minute") {
              if (num == "less than a") {
                now
              } else {
                now.minusMinutes(num.toInt)
              }
            } else {
              now
            }
          }
          case _ => now
        })).toList.head
        VisitorBadge(id.toInt, sanitize(name), "", when)
      }
    }))
      .filter { badge => !idsIHaveSeen.contains(badge.id) } // haven't seen it before
      .filter { badge => badge.created_at.isAfter(DateTime.now().minusMinutes(5)) } // created in the past two minutes
      .toList

    if (entries.length > 0) {
      println("PRINTING BADGES!")
      println(entries)
    }

    entries.foreach { badge =>
      idsIHaveSeen = idsIHaveSeen ++ Seq(badge.id)
    }

    entries
  }

}

object Main {
  def main(args: Array[String]) = {
    val p = new Properties()
    val home = System.getProperty("user.home")
    p.load(new FileInputStream(new File(home + "/badgeprinter.ini")))

    val conf = Configuration(
      printerName = p.getProperty("printerName"),
      badgeTemplate = p.getProperty("badgeTemplate").replaceAll("HOME", home),
      remoteDataSource = p.getProperty("remoteDataSource"),
      pollingFrequencyInUSec = p.getProperty("pollingFrequencyInUSec").toInt
    )

    val renderer = new BadgeRenderer(new URL("file:///" + conf.badgeTemplate))
    val printer = new BadgePrinter(conf.printerName, renderer)
    val data = new DataSource(conf.remoteDataSource)

    while (true) {
      try {
        data.poll().map { badge =>
          printer.print(badge)
        }

        Thread.sleep(conf.pollingFrequencyInUSec)
      } catch {
        case e: Exception => {
          println("ERROR: " + e)
          Thread.sleep(10000)
        }
      }
    }
  }
}


