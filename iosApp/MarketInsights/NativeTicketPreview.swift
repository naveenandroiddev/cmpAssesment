import SwiftUI
import MarketKitWrapper

struct NativeTicketPreview: View {

    let component: MarketInsightsComponent

    @StateObject private var stream: MarketBoardStream
    @State private var quantityText = "1000"
    @State private var limitPriceText = "99.500"
    @State private var side: Side = .buy
    @State private var orderType: OrderType = .limit
    @State private var summary: ValidationSummary?

    private let validator = SwiftTradeValidator(validator: TradeValidator())
    private let symbol = "US10Y"

    init(component: MarketInsightsComponent) {
        self.component = component
        _stream = StateObject(wrappedValue: MarketBoardStream(component: component))
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Live") {
                    LabeledContent("Instrument", value: symbol)
                    LabeledContent("Last", value: formattedLast)
                    LabeledContent("Phase", value: stream.board.map { String(describing: $0.phase) } ?? "—")
                }

                Section("Order") {
                    Picker("Side", selection: $side) {
                        Text("Buy").tag(Side.buy)
                        Text("Sell").tag(Side.sell)
                    }
                    .pickerStyle(.segmented)

                    Picker("Type", selection: $orderType) {
                        Text("Limit").tag(OrderType.limit)
                        Text("Market").tag(OrderType.market)
                    }
                    .pickerStyle(.segmented)

                    TextField("Quantity", text: $quantityText)
                        .keyboardType(.numberPad)
                        .monospacedDigit()

                    if orderType == .limit {
                        TextField("Limit price", text: $limitPriceText)
                            .keyboardType(.decimalPad)
                            .monospacedDigit()
                    }
                }

                if let summary {
                    Section("Pre-trade checks") {
                        if summary.isSubmittable && summary.warnings.isEmpty {
                            Label("Passes all checks", systemImage: "checkmark.seal")
                                .foregroundStyle(.green)
                        }
                        ForEach(summary.rejections, id: \.self) { message in
                            Label(message, systemImage: "xmark.octagon").foregroundStyle(.red)
                        }
                        ForEach(summary.warnings, id: \.self) { message in
                            Label(message, systemImage: "exclamationmark.triangle").foregroundStyle(.orange)
                        }
                    }
                }

                Button("Submit \(side == .buy ? "buy" : "sell")") { /* route to the order gateway */ }
                    .disabled(summary?.isSubmittable != true)
            }
            .navigationTitle("Ticket")
        }
        .onAppear { stream.start() }
        .onDisappear { stream.stop() }
        // Validation is re-run when the *inputs* change or when a new board arrives —
        // never on a timer, and never per tick inside a render pass.
        .onChange(of: quantityText) { _, _ in revalidate() }
        .onChange(of: limitPriceText) { _, _ in revalidate() }
        .onChange(of: side) { _, _ in revalidate() }
        .onChange(of: orderType) { _, _ in revalidate() }
        .onReceive(stream.$board.compactMap { $0 }) { _ in revalidate() }
    }

    private var formattedLast: String {
        guard let quote = currentQuote else { return "—" }
        return validator.formatPrice(micros: quote.last, decimals: 3)
    }

    private var currentQuote: Quote? {
        guard let board = stream.board else { return nil }
        return MarketKit.shared.quote(board: board, symbol: symbol)
    }

    private func revalidate() {
        guard
            let board = stream.board,
            let instrument = MarketKit.shared.instrument(component: component, symbol: symbol)
        else { return }


        let limitMicros = MarketKit.shared.parsePriceMicros(text: limitPriceText)

        summary = validator.validateOrder(
            instrument: instrument,
            quote: currentQuote,
            phase: board.phase,
            side: side,
            quantity: Int64(quantityText) ?? 0,
            orderType: orderType,
            limitPriceMicros: limitMicros,
            nowEpochMillis: Int64(Date().timeIntervalSince1970 * 1000)
        )
    }
}
