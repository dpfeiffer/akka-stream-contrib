/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.contrib

import akka.NotUsed
import akka.stream.scaladsl.{ Sink, Source }
import org.scalatest.concurrent.ScalaFutures

import scala.collection.immutable
import scala.concurrent.Future

class PagedSourceSpec extends BaseStreamSpec with ScalaFutures {
  override protected def autoFusing = false

  import system._

  case class Page[T](next: Option[Int], items: immutable.Seq[T])

  object PagedSource {

    def apply[T](
      f:         Int => Future[Page[T]],
      firstPage: Int                    = 0
    ): Source[T, NotUsed] = {
      val pageSource: Source[Page[T], NotUsed] = Source.unfoldAsync[Int, Page[T]](firstPage)(i => f(i).map {
        case nonEmptyPage @ Page(_, items) if items.nonEmpty => Some((i + 1) -> nonEmptyPage)
        case _ => None
      })
      pageSource.flatMapConcat(page => Source(page.items))
    }

  }

  case class MultiplesOfTwo(size: Option[Int] = None) {

    val itemsPerPage = 2

    def page(i: Int): Future[Page[Int]] =
      Future.successful {
        val indices = i * itemsPerPage until (i + 1) * itemsPerPage
        val filteredIndices = size match {
          case Some(sz) => indices.filter(_ < sz)
          case None     => indices
        }
        Page(Some(i + 1), filteredIndices.map(_ * 2))
      }

  }

  "PagedSource" should {
    "returns the items in the proper order" in {
      val source = PagedSource(i => MultiplesOfTwo().page(i))

      val result = source.take(3).runWith(Sink.seq)
      whenReady(result) { a =>
        a shouldBe List(0, 2, 4)
      }
    }

    "returns not more items then available" in {
      val source = PagedSource(i => MultiplesOfTwo(Some(4)).page(i))

      val result = source.take(10).runWith(Sink.seq)
      whenReady(result) { a =>
        a.length shouldBe 4
      }
    }
  }

}
