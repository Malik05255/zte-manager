# Honor 200 home UI reference

The active production UI uses native Compose layout without whole-screen scaling. Geometry is normalized by width, not by device height, so tall phones such as Honor 200 keep the same visual proportions.

Project-wide readable layout contract:
- shared header and bottom navigation use one sizing system across Home, Network, Tools, Logs, More, Towers and Bands;
- cards, typography, spacing and touch targets are intentionally larger and clearer;
- screens may scroll vertically instead of compressing content to fit above the fold;
- bottom navigation participates in normal layout and must never overlay or hide the final scroll item;
- the Home dashboard keeps the approved HAI visual language while using larger reference row heights;
- truth-first behavior remains unchanged: unverified radio facts are not presented as verified values.
