import Foundation
import Combine
@_exported import MarketKit

@MainActor
public final class MarketBoardStream: ObservableObject {

    @Published public private(set) var board: QuoteBoard?

    private let subscription: MarketBoardSubscription
    private var cancellable: KotlinCancellable?

    public init(component: MarketInsightsComponent) {
        self.subscription = MarketBoardSubscription(repository: component.marketDataRepository)
    }

    public func start() {
        guard cancellable == nil else { return }
        cancellable = subscription.subscribe { [weak self] board in
            self?.board = board
        }
    }

    public func stop() {
        cancellable?.cancel()
        cancellable = nil
    }

    deinit {
        subscription.close()
    }
}

public extension MarketBoardSubscription {
    func boards() -> AsyncStream<QuoteBoard> {
        AsyncStream { continuation in
            let token = subscribe { board in continuation.yield(board) }
            continuation.onTermination = { _ in token.cancel() }
        }
    }
}
