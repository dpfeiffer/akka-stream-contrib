package akka.stream.contrib

import akka.NotUsed
import akka.stream.scaladsl.Source

import scala.collection.immutable
import scala.concurrent.Future

object PagedSource {

  import scala.concurrent.ExecutionContext.Implicits.global // todo

  case class Page[K, T](nextKey: Option[K], items: immutable.Iterable[T])

  def apply[K, T](firstKey: K)(f: K => Future[Page[K, T]]): Source[T, NotUsed] = {
    val pageSource: Source[Page[K, T], NotUsed] =
      Source.unfoldAsync[Option[K], Page[K, T]](Some(firstKey)) { key =>
        val pageFuture: Future[Page[K, T]] = key match {
          case Some(k) => f(k)
          case None => Future.successful(Page(None, immutable.Seq.empty))
        }
        pageFuture.map {
          case nonEmptyPage @ Page(nextKey, items) if items.nonEmpty => Some(nextKey -> nonEmptyPage)
          case _ => None
        }
      }
    pageSource.flatMapConcat(page => Source(page.items))
  }

}
