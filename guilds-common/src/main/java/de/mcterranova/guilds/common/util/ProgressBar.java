package de.mcterranova.guilds.common.util;

import org.jetbrains.annotations.NotNull;

public class ProgressBar {

    /**
     * Creates a progress bar using partial block characters.
     * <p>
     * Each bar segment can contain:
     * - A full filled block.
     * - A partial filled block followed by a partial empty block.
     * <p>
     * This ensures that each segment occupies consistent space in the GUI.
     *
     * @param current the current progress (e.g., 55)
     * @param max     the required total (e.g., 100)
     * @return a String representing the progress bar, e.g., "§a█████▌▏████▉▎████"
     */
    public static String createProgressBar(int current, int max) {
        if (max <= 0) {
            return "§a" + "█".repeat(10);
        }

        current = Math.min(current, max);

        double progressFraction = (double) current / max;
        double totalPartials = 10 * 8;
        double progressPartial = progressFraction * totalPartials;

        int fullSegments = (int) (progressPartial / 8);
        int partialValue = (int) (progressPartial % 8);

        StringBuilder bar = getStringBuilder(fullSegments, partialValue);

        return bar.toString();
    }

    private static @NotNull StringBuilder getStringBuilder(int fullSegments, int partialValue) {
        StringBuilder bar = new StringBuilder("§a");

        bar.append("█".repeat(Math.max(0, fullSegments)));

        if (partialValue > 0 && fullSegments < 10) {
            bar.append(getPartialBlock(partialValue));
            bar.append("§7");
            bar.append(getPartialBlock(8 - partialValue + 1));
        }

        int filledLength = fullSegments + (partialValue > 0 ? 1 : 0);
        int remaining = 10 - filledLength;
        bar.append("§7");
        bar.append("█".repeat(Math.max(0, remaining)));
        return bar;
    }

    /**
     * Returns a partial block character based on the fractional value.
     *
     * @param partialValue an integer from 1 to 7 representing the fraction (1/8 to 7/8)
     * @return a String containing the partial block character
     */
    private static String getPartialBlock(int partialValue) {
        return switch (partialValue) {
            case 1 -> "▏"; // 1/8
            case 2 -> "▎"; // 2/8
            case 3 -> "▍"; // 3/8
            case 4 -> "▌"; // 4/8
            case 5 -> "▋"; // 5/8
            case 6 -> "▊"; // 6/8
            case 7 -> "▉"; // 7/8
            default -> "";   // No partial block
        };
    }
}
