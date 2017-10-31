package akka.stream.contrib

import akka.NotUsed
import akka.stream.scaladsl.Source

import scala.collection.immutable
import scala.concurrent.Future

object PagedSource {

  import scala.concurrent.ExecutionContext.Implicits.global // todo

  case class Page[K, T](nextKey: Option[K], items: immutable.Iterable[T])

  def apply[K, T](
    f: Option[K] => Future[Page[K, T]],
    firstKey: K
  ): Source[T, NotUsed] = {
    val pageSource: Source[Page[K, T], NotUsed] =
      Source.unfoldAsync[Option[K], Page[K, T]](Some(firstKey))(key => f(key).map {
        case nonEmptyPage @ Page(nextKey, items) if items.nonEmpty => Some(nextKey -> nonEmptyPage)
        case _ => None
      })
    pageSource.flatMapConcat(page => Source(page.items))
  }

}

class PagedSource {

}
