package noppes.npcs.api.item;

/**
 * Empty marker: SamuraiAI never touches item stacks through the CustomNPCs
 * API, but several methods it does call ({@code ICustomNpc.shootItem},
 * {@code giveItem}, ...) mention this type in their signature. See
 * {@link noppes.npcs.api.entity.IMob} for why trimming a stub interface down
 * like this is safe.
 */
public interface IItemStack {
}
