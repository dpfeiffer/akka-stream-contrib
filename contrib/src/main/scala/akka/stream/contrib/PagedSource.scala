package akka.stream.contrib

import akka.NotUsed
import akka.stream.scaladsl.Source

import scala.collection.immutable
import scala.concurrent.Future

object PagedSource {

  import scala.concurrent.ExecutionContext.Implicits.global // todo

  case class Page[T](next: Option[Int], items: immutable.Seq[T])

  def apply[T](
    f: Int => Future[Page[T]],
    firstPage: Int = 0
  ): Source[T, NotUsed] = {
    val pageSource: Source[Page[T], NotUsed] = Source.unfoldAsync[Int, Page[T]](firstPage)(i => f(i).map {
      case nonEmptyPage @ Page(_, items) if items.nonEmpty => Some((i + 1) -> nonEmptyPage)
      case _ => None
    })
    pageSource.flatMapConcat(page => Source(page.items))
  }

}

class PagedSource {

}
