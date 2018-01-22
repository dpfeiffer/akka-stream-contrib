/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.stream.contrib

import akka.NotUsed
import akka.japi.function
import akka.stream.scaladsl.Source

import scala.collection.immutable
import scala.concurrent.{ ExecutionContext, Future }

/**
  * This object defines a factory for [[PagedSource]] instances, see [[PagedSource.apply]].
  */
object PagedSource {

  type PagedSource[T] = Source[T, NotUsed]

  case class Page[T, K](items: immutable.Iterable[T], nextKey: Option[K])

  /**
    * Factory for [[PagedSource]] instances.
    *
    * @param firstKey key of first page
    * @param f        map page key to Future of page data
    * @param executor execution context for futures
    * @tparam T type of page data
    * @tparam K type of page keys
    * @return [[PagedSource]] instance
    */
  def apply[T, K](firstKey: K)(f: K => Future[Page[T, K]])(implicit executor: ExecutionContext): PagedSource[T] = {
    val pageSource: PagedSource[Page[T, K]] =
      Source.unfoldAsync[Option[K], Page[T, K]](Some(firstKey)) { key =>
        val pageFuture: Future[Page[T, K]] = key match {
          case Some(k) => f(k)
          case None => Future.successful(Page(immutable.Seq.empty, None))
        }
        pageFuture.map {
          case nonEmptyPage @ Page(items, nextKey) if items.nonEmpty => Some(nextKey -> nonEmptyPage)
          case _ => None
        }
      }
    pageSource.flatMapConcat(page => Source(page.items))
  }

  /**
    * Java API: Factory for [[PagedSource]] instances.
    *
    * @param firstKey key of first page
    * @param f        map page key to Future of page data
    * @param executor execution context for futures
    * @tparam T type of page data
    * @tparam K type of page keys
    * @return [[PagedSource]] instance
    */
  def create[T, K](firstKey: K, f: function.Function[K, Future[Page[T, K]]], executor: ExecutionContext): PagedSource[T] =
    PagedSource[T, K](firstKey)(f.apply)(executor)

}
