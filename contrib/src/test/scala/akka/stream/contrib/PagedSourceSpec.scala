/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.contrib

import org.scalatest.concurrent.ScalaFutures

import akka.stream.scaladsl.Sink

import scala.collection.immutable
import scala.concurrent.Future

class PagedSourceSpec extends BaseStreamSpec with ScalaFutures {
  override protected def autoFusing = false

  case class MultiplesOfTwo(size: Option[Int] = None) {

    val itemsPerPage = 2

    def page(key: Option[Int]): Future[PagedSource.Page[Int, Int]] = key match {
      case Some(i) =>
        Future.successful {
          val indices = i * itemsPerPage until (i + 1) * itemsPerPage
          val filteredIndices = size match {
            case Some(sz) => indices.filter(_ < sz)
            case None => indices
          }
          PagedSource.Page(Some(i + 1), filteredIndices.map(_ * 2))
        }
      case None => Future.successful(PagedSource.Page(None, immutable.Seq.empty))
    }
  }

  "PagedSource" should {
    "returns the items in the proper order" in {
      val source = PagedSource(i => MultiplesOfTwo().page(i), 0)

      val result = source.take(3).runWith(Sink.seq)
      whenReady(result) { a =>
        a shouldBe List(0, 2, 4)
      }
    }

    "returns not more items then available" in {
      val source = PagedSource(i => MultiplesOfTwo(Some(4)).page(i), 0)

      val result = source.take(10).runWith(Sink.seq)
      whenReady(result) { a =>
        a.length shouldBe 4
      }
    }
  }

}
