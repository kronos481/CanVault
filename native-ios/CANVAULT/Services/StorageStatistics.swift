import Foundation

enum StorageStatistics {
    static func calculate(cans: [CanItem], now: Date = Date(), catalog: CatalogStore = .shared) -> StorageStats {
        let current = cans.filter { $0.status != .archived }
        let priced = cans.compactMap(\.purchasePriceCents)
        let remaining: [String: Int] = Dictionary(uniqueKeysWithValues: cans.map { ($0.id, remainingMl($0)) })
        let used: [String: Int] = Dictionary(uniqueKeysWithValues: cans.map {
            ($0.id, max(($0.volumeMl ?? 400) - (remaining[$0.id] ?? 0), 0))
        })
        let completed = cans.filter { $0.status == .archived || $0.status == .empty || $0.fillPercent == 0 }
        let usageDays = completed.compactMap { can -> Double? in
            let end = can.archivedAt ?? can.updatedAt
            guard end >= can.acquiredAt else { return nil }
            return Double(end - can.acquiredAt) / 86_400_000
        }
        let calendar = Calendar.current
        let startOfCurrentMonth = calendar.date(from: calendar.dateComponents([.year, .month], from: now)) ?? now
        let months = (0..<6).reversed().compactMap { calendar.date(byAdding: .month, value: -$0, to: startOfCurrentMonth) }
        let monthly = months.map { month in
            let next = calendar.date(byAdding: .month, value: 1, to: month) ?? month
            let entries = cans.filter {
                let date = Date(millisecondsSince1970: $0.acquiredAt)
                return date >= month && date < next
            }
            return StorageMonthStat(month: month, addedCanCount: entries.count, spentCents: entries.reduce(0) { $0 + ($1.purchasePriceCents ?? 0) })
        }
        let usedByColor = Dictionary(grouping: cans, by: { $0.colorName.nilIfBlank ?? "Unbenannte Farbe" })
            .mapValues { entries in entries.reduce(0) { $0 + (used[$1.id] ?? 0) } }
        let topUsed = usedByColor.filter { $0.value > 0 }.max { first, second in
            first.value == second.value ? first.key > second.key : first.value < second.value
        }?.key
        let distinct = Set(cans.map { catalog.resolveHex(for: $0)?.uppercased() ?? $0.colorName.lowercased() }.filter { !$0.isEmpty })
        let unopened = current.isEmpty ? 0 : Int((Double(current.filter { $0.status == .inStock && ($0.fillPercent ?? 100) == 100 }.count) * 100 / Double(current.count)).rounded())

        return StorageStats(
            allTimeCanCount: cans.count,
            currentCanCount: current.count,
            archivedCanCount: cans.filter { $0.status == .archived }.count,
            emptyCanCount: cans.filter { $0.fillPercent == 0 || $0.status == .empty || $0.statusBeforeArchive == .empty }.count,
            totalSpentCents: priced.reduce(0, +),
            currentInventoryValueCents: current.reduce(0) { $0 + ($1.purchasePriceCents ?? 0) },
            averagePriceCents: priced.isEmpty ? nil : Int((Double(priced.reduce(0, +)) / Double(priced.count)).rounded()),
            mostExpensiveCanCents: priced.max(),
            purchasedVolumeMl: cans.reduce(0) { $0 + max($1.volumeMl ?? 400, 0) },
            currentRemainingVolumeMl: current.reduce(0) { $0 + (remaining[$1.id] ?? 0) },
            estimatedUsedVolumeMl: used.values.reduce(0, +),
            distinctColorCount: distinct.count,
            unopenedPercent: unopened,
            averageUsageDays: usageDays.isEmpty ? nil : Int((usageDays.reduce(0, +) / Double(usageDays.count)).rounded()),
            topBrandId: mostFrequent(cans.map(\.brandId)),
            topLineId: mostFrequent(cans.map(\.canLineId)),
            topUsedColorName: topUsed,
            monthlyActivity: monthly
        )
    }

    static func remainingMl(_ can: CanItem) -> Int {
        Int((Double(max(can.volumeMl ?? 400, 0)) * Double(min(max(can.fillPercent ?? 100, 0), 100)) / 100).rounded())
    }

    private static func mostFrequent(_ values: [String]) -> String? {
        var counts: [String: Int] = [:]
        for value in values where !value.isEmpty {
            counts[value, default: 0] += 1
        }
        let ranked = counts.sorted { first, second in
            if first.value != second.value { return first.value > second.value }
            return first.key < second.key
        }
        return ranked.first?.key
    }
}
