package foundation.e.blisslauncher.core.customviews;

import android.view.View;
import android.view.WindowInsets;

/**
 * Allows the implementing {@link View} to not draw underneath system bars.
 * e.g., notification bar on top and home key area on the bottom.
 */
public interface Insettable {

    void setInsets(WindowInsets insets);
}
