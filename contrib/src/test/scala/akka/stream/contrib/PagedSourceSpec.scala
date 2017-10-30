/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.contrib

import org.scalatest.concurrent.ScalaFutures

import akka.stream.scaladsl.Sink

import scala.concurrent.Future

class PagedSourceSpec extends BaseStreamSpec with ScalaFutures {
  override protected def autoFusing = false

  case class MultiplesOfTwo(size: Option[Int] = None) {

    val itemsPerPage = 2

    def page(i: Int): Future[PagedSource.Page[Int]] =
      Future.successful {
        val indices = i * itemsPerPage until (i + 1) * itemsPerPage
        val filteredIndices = size match {
          case Some(sz) => indices.filter(_ < sz)
          case None => indices
        }
        PagedSource.Page(Some(i + 1), filteredIndices.map(_ * 2))
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
