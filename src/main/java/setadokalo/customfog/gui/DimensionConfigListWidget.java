package setadokalo.customfog.gui;

import java.util.Objects;

import com.mojang.blaze3d.systems.RenderSystem;

import org.apache.logging.log4j.Level;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import setadokalo.customfog.CustomFog;

public class DimensionConfigListWidget extends AlwaysSelectedEntryListWidget<DimensionConfigEntry> {
	private Screen parent;
	private boolean renderSelection;
	private TextRenderer textRenderer;
	private boolean scrolling;

	public DimensionConfigListWidget(MinecraftClient minecraftClient, int x, int y, int width, int height,
			int itemheight, Screen parent, TextRenderer textRenderer) {
		super(minecraftClient, width, height, y, y + height, itemheight);
		this.left = x;
		this.right = x + width;
		this.top = y;
		this.bottom = y + height;
		this.height = parent.height;
		this.setRenderSelection(false);
		this.parent = parent;
		this.textRenderer = textRenderer;
		//this.method_31323(false); // this disables rendering a background above and below the scroll list - useful for debugging
	}

	public void tick() {
		for (DimensionConfigEntry entry : this.children()) {
			entry.tick();
		}
	}

	public TextRenderer getTextRenderer() {
		return textRenderer;
	}

	public Screen getParent() {
		return parent;
	}

	public void add(DimensionConfigEntry entry) {
		this.addEntry(entry);
	}
	public void remove(DimensionConfigEntry entry) {
		this.removeEntry(entry);
	}

	@Override
	public void setRenderSelection(boolean renderSelection) {
		super.setRenderSelection(renderSelection);
		this.renderSelection = renderSelection;
	}

	@Override
   public boolean changeFocus(boolean lookForwards) {
		CustomFog.log(Level.INFO, "Focus Changed on list widget");
		return super.changeFocus(lookForwards);
	}

	@Override
	protected void renderList(MatrixStack matrices, int x, int y, int mouseX, int mouseY, float delta) {
		int itemCount = this.getItemCount();
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		for (int index = 0; index < itemCount; ++index) {
			int entryTop = this.getRowTop(index) + 2;
			int entryBottom = this.getRowTop(index) + this.itemHeight;
			if (entryBottom >= this.top && entryTop <= this.bottom) {
				int entryHeight = this.itemHeight - 4;
				DimensionConfigEntry entry = this.getEntry(index);
				int rowWidth = this.getRowWidth();
				int entryLeft;
				if (this.renderSelection && this.isSelectedItem(index)) {
					entryLeft = getRowLeft() - 2;
					int selectionRight = x + rowWidth + 2;
					RenderSystem.disableTexture();
					float bgIntensity = this.isFocused() ? 1.0F : 0.5F;
					RenderSystem.color4f(bgIntensity, bgIntensity, bgIntensity, 1.0F);
					Matrix4f matrix = matrices.peek().getModel();
					buffer.begin(7, VertexFormats.POSITION);
					buffer.vertex(matrix, entryLeft, entryTop + entryHeight + 2, 0.0F).next();
					buffer.vertex(matrix, selectionRight, entryTop + entryHeight + 2, 0.0F).next();
					buffer.vertex(matrix, selectionRight, entryTop - 2, 0.0F).next();
					buffer.vertex(matrix, entryLeft, entryTop - 2, 0.0F).next();
					tessellator.draw();
					RenderSystem.color4f(0.0F, 0.0F, 0.0F, 1.0F);
					buffer.begin(7, VertexFormats.POSITION);
					buffer.vertex(matrix, entryLeft + 1, entryTop + entryHeight + 1, 0.0F).next();
					buffer.vertex(matrix, selectionRight - 1, entryTop + entryHeight + 1, 0.0F).next();
					buffer.vertex(matrix, selectionRight - 1, entryTop - 1, 0.0F).next();
					buffer.vertex(matrix, entryLeft + 1, entryTop - 1, 0.0F).next();
					tessellator.draw();
					RenderSystem.enableTexture();
				}

				entryLeft = this.getRowLeft();
				entry.render(matrices, index, entryTop, entryLeft, rowWidth, entryHeight, mouseX, mouseY, this.isMouseOver(mouseX, mouseY) && Objects.equals(this.getEntryAtPos(mouseX, mouseY), entry), delta);
			}
		}

	}

	// // We're overriding this so that when it calls `Entry#mouseClicked` it localizes the mouseY coordinate
	@Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      this.updateScrollingState(mouseX, mouseY, button);
      if (!this.isMouseOver(mouseX, mouseY)) {
         return false;
      } else {
			for (DimensionConfigEntry entry : this.children()) {
				if (entry.dimNameWidget != null)
					entry.dimNameWidget.setSelected(false);
			}
			DimensionConfigEntry entry = this.getEntryAtPosition(mouseX, mouseY);
         if (entry != null) {
				this.setSelected(entry);
            if (entry.mouseClicked(mouseX, mouseY, button)) {
					this.setFocused(entry);
               this.setDragging(true);
               return true;
            }
         } else if (button == 0) {
            this.clickedHeader((int)(mouseX - (double)(this.left + this.width / 2 - this.getRowWidth() / 2)), (int)(mouseY - (double)this.top) + (int)this.getScrollAmount() - 4);
            return true;
         }

         return this.scrolling;
      }
   }


	public final int toEntryPos(double yScreenspace) {
		int heightInList = MathHelper.floor(yScreenspace - (double) this.top) - this.headerHeight + (int) this.getScrollAmount() - 4;
		return heightInList % this.itemHeight;
	}
	
	public final int toEntryIndex(double yScreenspace) {
		int heightInList = MathHelper.floor(yScreenspace - (double) this.top) - this.headerHeight + (int) this.getScrollAmount() - 4;
		return heightInList / this.itemHeight;
	}

	public final DimensionConfigEntry getEntryAtPos(double x, double y) {
		int heightInList = MathHelper.floor(y - (double) this.top) - this.headerHeight + (int) this.getScrollAmount() - 4;
		int index = heightInList / this.itemHeight;
		return x < (double) this.getScrollbarPositionX() && x >= (double) getRowLeft() && x <= (double) (getRowLeft() + getRowWidth()) && index >= 0 && heightInList >= 0 && index < this.getItemCount() ? this.children().get(index) : null;
	}

	@Override
   protected void updateScrollingState(double mouseX, double mouseY, int button) {
		this.scrolling = button == 0 && mouseX >= (double)this.getScrollbarPositionX() && mouseX < (double)(this.getScrollbarPositionX() + 6);
		super.updateScrollingState(mouseX, mouseY, button);
	}

	@Override
   protected int getScrollbarPositionX() {
      return this.right - 6;
	}
	

	@Override
	public int getRowWidth() {
		return this.width - (Math.max(0, this.getMaxPosition() - (this.bottom - this.top - 4)) > 0 ? 18 : 12);
	}

	@Override
	public int getRowLeft() {
		return left + 6;
	}
}
