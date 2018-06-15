/**
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package com.lightbend.paradox.unidoc

import com.lightbend.paradox.markdown.Writer

class UnidocPluginSpec extends MarkdownBaseSpec {
  val rootPackage = "akka"

  val allClasses = Array(
    "akka.actor.ActorRef",
    "akka.actor.typed.ActorRef",
    "akka.dispatch.Envelope",
    "akka.stream.javadsl.Flow",
    "akka.stream.scaladsl.Flow",
  )

  override val markdownWriter = new Writer(
    linkRenderer = Writer.defaultLinks,
    verbatimSerializers = Writer.defaultVerbatims,
    serializerPlugins = Writer.defaultPlugins(
      Writer.defaultDirectives ++ Seq(
        (_: Writer.Context) => new UnidocDirective(allClasses)
      )
    )
  )

  implicit val context = writerContextWithProperties(
    "scaladoc.akka.base_url" -> "https://doc.akka.io/api/akka/2.5",
    "scaladoc.akka.http.base_url" -> "https://doc.akka.io/api/akka-http/current",
    "javadoc.akka.base_url" -> "https://doc.akka.io/japi/akka/2.5",
    "javadoc.akka.http.base_url" -> "http://doc.akka.io/japi/akka-http/current",
  )

  "Unidoc directive" should "generate markdown correctly when there is only one match" in {
    markdown("@unidoc[Envelope]") shouldEqual
      html(
        """<p><span class="group-scala">
          |<a href="https://doc.akka.io/api/akka/2.5/akka/dispatch/Envelope.html">Envelope</a></span><span class="group-java">
          |<a href="https://doc.akka.io/japi/akka/2.5/?akka/dispatch/Envelope.html">Envelope</a></span>
          |</p>""".stripMargin
      )
  }

  it should "throw an exception when there is no match" in {
    val thrown = the[IllegalStateException] thrownBy markdown("@unidoc[ThereIsNoSuchClass]")
    thrown.getMessage shouldEqual
      "No matches found for ThereIsNoSuchClass"
  }


  it should "generate markdown correctly when 2 matches found and their package names include javadsl/scaladsl" in {
    markdown("@unidoc[Flow]") shouldEqual
      html(
        """<p><span class="group-java">
          |<a href="https://doc.akka.io/japi/akka/2.5/?akka/stream/javadsl/Flow.html">Flow</a></span><span class="group-scala">
          |<a href="https://doc.akka.io/api/akka/2.5/akka/stream/scaladsl/Flow.html">Flow</a></span>
          |</p>""".stripMargin
      )
  }

  it should "throw an exception when two matches found but javadsl/scaladsl is not in their packages" in {
    val thrown = the[IllegalStateException] thrownBy markdown("@unidoc[ActorRef]")
    thrown.getMessage shouldEqual
      "2 matches found for ActorRef, but not javadsl/scaladsl: akka.actor.ActorRef, akka.actor.typed.ActorRef. You may want to use the fully qualified class name as @unidoc[fqcn] instead of @unidoc[ActorRef]."
  }

  it should "generate markdown correctly when fully qualified class name (fqcn) is specified as @unidoc[fqcn]" in {
    markdown("@unidoc[akka.actor.ActorRef]") shouldEqual
      html(
        """<p><span class="group-scala">
          |<a href="https://doc.akka.io/api/akka/2.5/akka/actor/ActorRef.html">ActorRef</a></span><span class="group-java">
          |<a href="https://doc.akka.io/japi/akka/2.5/?akka/actor/ActorRef.html">ActorRef</a></span>
          |</p>""".stripMargin
      )
  }

  it should "" in {
    markdown("@unidoc[akka.cluster.client.ClusterClient$]") shouldEqual
      html(
        """<p><span class="group-scala">
          |<a href="https://doc.akka.io/api/akka/2.5/akka/cluster/client/ClusterClient$$Publish.html">ClusterClient.Publish</a></span><span class="group-java">
          |<a href="https://doc.akka.io/japi/akka/2/akka/cluster/client/ClusterClient.Publish.html">ClusterClient.Publish</a></span>
          |</p>""".stripMargin
      )
  }
}
