# Retrieval Inventory

When a player's chest gets **smaller** (rank expired, permission removed), the items above the new row count are not destroyed. They stay stored and can be opened with:

```
/retrieveender
/retrieveender <player>
```

| Permission | Allows |
|---|---|
| `enderchest.retrieve` | open your own retrieval inventory |
| `enderchest.retrieve.others` | open another player's |

Items can only be **taken out** of the retrieval inventory, not put in.

## Messages

- `nothing-to-retrieve-self` / `nothing-to-retrieve-others` — shown when there is nothing left.
- The inventory title is `configurable.enderchest-names.retrieval` in the lang file.

## From the console

`/retrieveender <player> [other player]` opens the retrieval inventory of *other player* for *player*. Both must be online for the viewer; see [Commands](../commands-and-permissions.md).
