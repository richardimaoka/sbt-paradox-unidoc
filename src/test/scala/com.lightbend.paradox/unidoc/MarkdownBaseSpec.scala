/**
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */


package com.lightbend.paradox.unidoc

import com.lightbend.paradox.tree.Tree.{Forest, Location}
import java.io.{File, PrintWriter}

import org.scalatest.{FlatSpec, Matchers}
import com.lightbend.paradox.template.PageTemplate
import java.nio.file._

import com.lightbend.paradox.markdown._

abstract class MarkdownBaseSpec extends FlatSpec with Matchers {

  val markdownReader = new Reader
  val markdownWriter = new Writer

  def markdown(text: String)(implicit context: Location[Page] => Writer.Context = writerContext): String = {
    markdownPages("test.md" -> text).getOrElse("test.html", "")
  }

  def markdownPages(mappings: (String, String)*)(implicit context: Location[Page] => Writer.Context = writerContext): Map[String, String] = {
    def render(location: Option[Location[Page]], rendered: Seq[(String, String)] = Seq.empty): Seq[(String, String)] = location match {
      case Some(loc) =>
        val page = loc.tree.label
        val html = normalize(markdownWriter.write(page.markdown, context(loc)))
        render(loc.next, rendered :+ (page.path, html))
      case None => rendered
    }
    render(Location.forest(pages(mappings: _*))).toMap
  }

  def layoutPages(mappings: (String, String)*)(templates: (String, String)*)(implicit context: Location[Page] => Writer.Context = writerContext): Map[String, String] = {
    val templateDirectory = Files.createTempDirectory("templates")
    createFileTemplates(templateDirectory, templates)
    def render(location: Option[Location[Page]], rendered: Seq[(String, String)] = Seq.empty): Seq[(String, String)] = location match {
      case Some(loc) =>
        val page = loc.tree.label
        val html = normalize(markdownWriter.write(page.markdown, context(loc)))
        val outputFile = new File(page.path)
        val emptyPageContext = PartialPageContent(page.properties.get, html)
        val template = new PageTemplate(new File(templateDirectory.toString))
        template.write(page.properties(Page.Properties.DefaultLayoutMdIndicator, template.defaultName), emptyPageContext, outputFile, new PageTemplate.ErrorLogger(s => println("[error] " + s)))
        val fileContent = fileToContent(outputFile)
        outputFile.delete
        render(loc.next, rendered :+ (page.path, normalize(fileContent)))
      case None => rendered
    }
    render(Location.forest(pages(mappings: _*))).toMap
  }

  def fileToContent(file: File): String = {
    import scala.io.Source
    Source.fromFile(file).getLines.mkString("\n")
  }

  def createFileTemplates(dir: Path, templates: Seq[(String, String)]) = {
    val suffix = ".st"
    (templates map {
      case (path, content) if (path.endsWith(suffix)) =>
        val writer = new PrintWriter(new File(dir.toString + "/" + path))
        writer.write(prepare(content))
        writer.close()
    })
  }

  def writerContextWithProperties(properties: (String, String)*): Location[Page] => Writer.Context = { location =>
    writerContext(location).copy(properties = properties.toMap)
  }

  def writerContext(location: Location[Page]): Writer.Context = {
    Writer.Context(
      location,
      Page.allPaths(List(location.root.tree)).toSet,
      groups = Map("Language" -> Seq("Scala", "Java"))
    )
  }

  def pages(mappings: (String, String)*): Forest[Page] = {
    import com.lightbend.paradox.markdown.Path
    val parsed = mappings map {
      case (path, text) =>
        val frontin = Frontin(prepare(text))
        (new File(path), path, markdownReader.read(frontin.body), frontin.header)
    }
    Page.forest(parsed, Path.replaceSuffix(Writer.DefaultSourceSuffix, Writer.DefaultTargetSuffix))
  }

  def html(text: String): String = {
    normalize(prepare(text))
  }

  def htmlPages(mappings: (String, String)*): Map[String, String] = {
    (mappings map { case (path, text) => (path, html(text)) }).toMap
  }

  def prepare(text: String): String = {
    text.stripMargin.trim
  }

  def normalize(html: String) = {
    val reader = new java.io.StringReader(html)
    val writer = new java.io.StringWriter
    val tidy = new org.w3c.tidy.Tidy
    tidy.setTabsize(2)
    tidy.setPrintBodyOnly(true)
    tidy.setTrimEmptyElements(false)
    tidy.setShowWarnings(false)
    tidy.setQuiet(true)
    tidy.parse(reader, writer)
    writer.toString.replace("\r\n", "\n").replace("\r", "\n")
  }

  case class PartialPageContent(properties: Map[String, String], content: String) extends PageTemplate.Contents {
    import scala.collection.JavaConverters._

    val getTitle = ""
    val getContent = content

    lazy val getBase = ""
    lazy val getHome = new EmptyLink()
    lazy val getPrev = new EmptyLink()
    lazy val getNext = new EmptyLink()
    lazy val getBreadcrumbs = ""
    lazy val getNavigation = ""
    lazy val hasSubheaders = false
    lazy val getToc = ""
    lazy val getSource_url = ""

    lazy val getProperties = properties.asJava
  }

  case class EmptyLink() extends PageTemplate.Link {
    lazy val getHref: String = ""
    lazy val getHtml: String = ""
    lazy val getTitle: String = ""
    lazy val isActive: Boolean = false
  }

}
