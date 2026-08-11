type StatCardProps = {
	icon: string;
	title: string;
	subtitle: string;
	className?: string;
	filledIcon?: boolean;
};

export function StatCard({ icon, title, subtitle, className = "", filledIcon = false }: StatCardProps) {
	return (
		<div className={`rounded-[2rem] p-5 aspect-square flex flex-col justify-between ${className}`}>
			<span
				className="material-symbols-outlined text-4xl"
				style={{ fontVariationSettings: filledIcon ? "'FILL' 1, 'wght' 400, 'GRAD' 0, 'opsz' 24" : undefined }}
			>
				{icon}
			</span>

			<div>
				<p className="headline-font text-4xl font-black">{title}</p>
				<p className="text-xs uppercase font-bold tracking-wide opacity-80">{subtitle}</p>
			</div>
		</div>
	);
}
