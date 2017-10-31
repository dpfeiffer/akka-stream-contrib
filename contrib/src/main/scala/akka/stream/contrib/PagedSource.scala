package akka.stream.contrib

import akka.NotUsed
import akka.stream.scaladsl.Source

import scala.collection.immutable
import scala.concurrent.{ ExecutionContext, Future }

object PagedSource {

  case class Page[T, K](items: immutable.Iterable[T], nextKey: Option[K])

  def apply[T, K](firstKey: K)(f: K => Future[Page[T, K]])(implicit executor: ExecutionContext): Source[T, NotUsed] = {
    val pageSource: Source[Page[T, K], NotUsed] =
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

}
