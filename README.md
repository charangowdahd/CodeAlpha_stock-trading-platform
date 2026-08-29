## Getting Started

This project is a desktop stock trading simulator. It includes market data, buy and sell validation, portfolio valuation, performance history, and CSV persistence.

Compile and run it from the project root:

```text
javac -d bin src\App.java
java -cp bin App
```

The simulator starts a desktop window with $10,000 and sample prices for AAPL, MSFT, TSLA, and NVDA. Use the ticker, quantity, Buy, Sell, Update price, and Refresh controls to manage the portfolio. The profit/loss chart plots each saved trade snapshot above or below the zero line. Trade snapshots are appended to `portfolio_history.csv` when a trade is completed.

The model is organized around `Stock`, `Market`, `User`, `Transaction`, `Portfolio`, and `PortfolioRepository` classes.

## Folder Structure

The workspace contains two folders by default, where:

- `src`: the folder to maintain sources
- `lib`: the folder to maintain dependencies

Meanwhile, the compiled output files will be generated in the `bin` folder by default.

> If you want to customize the folder structure, open `.vscode/settings.json` and update the related settings there.

## Dependency Management

The `JAVA PROJECTS` view allows you to manage your dependencies. More details can be found [here](https://github.com/microsoft/vscode-java-dependency#manage-dependencies).
