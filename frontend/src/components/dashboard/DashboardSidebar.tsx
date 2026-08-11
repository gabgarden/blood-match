import { Link } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { hasDonorRole, hasRequesterRole, hasAdminRole } from "../../routes/roleRouting";

type DonorDashboardSidebarProps = {
  onLogout: () => void;
  activeItem?: "donor-dashboard" | "requests" | "new-request" | "external-donation" | "profile";
};

type MenuItem = {
  key: "donor-dashboard" | "requests" | "new-request" | "profile";
  icon: string;
  label: string;
  path: string;
};

export function DonorDashboardSidebar({
  onLogout,
  activeItem = "donor-dashboard",
}: DonorDashboardSidebarProps) {
  const { roles } = useAuth();
  const canAccessRequesterArea = hasRequesterRole(roles);
  const canAccessDonorDashboard = hasDonorRole(roles);
  const canAccessAdminArea = hasAdminRole(roles);
  const showDonorDashboard = canAccessDonorDashboard || canAccessAdminArea;

  const menuItems: MenuItem[] = [
    ...(showDonorDashboard
      ? ([{ key: "donor-dashboard", icon: "dashboard", label: "Dashboard", path: "/dashboard" }] as MenuItem[])
      : []),
    ...(canAccessRequesterArea || canAccessAdminArea
      ? ([
          { key: "requests", icon: "assignment", label: "Suas Requisições", path: "/requests" },
          { key: "new-request", icon: "add_circle", label: "Nova Requisição", path: "/requests/new" },
        ] as MenuItem[])
      : []),
    { key: "profile", icon: "person", label: "Meu Perfil", path: "/profile" },
  ];

  return (
    <aside className="hidden lg:flex fixed left-0 top-0 z-40 h-screen w-64 flex-col border-r border-surface-container-high bg-[#f3f3f5] p-4">
      <div className="mb-8 px-2 py-4">
        <h1 className="font-headline text-xl font-black text-primary">BloodMatch</h1>
        <p className="text-[10px] font-bold uppercase tracking-widest text-secondary">Ajude a salvar vidas</p>
      </div>

      <nav className="flex flex-grow flex-col gap-1.5">
        {menuItems.map((item) => (
          <Link
            key={item.label}
            to={item.path}
            className={`flex items-center gap-3 rounded-xl px-4 py-3 text-left text-sm font-medium transition-all ${
              item.key === activeItem
                ? "bg-white text-primary shadow-sm"
                : "text-text-secondary hover:bg-[#f9f9fb] hover:text-primary"
            }`}
          >
            <span className="material-symbols-outlined">{item.icon}</span>
            <span>{item.label}</span>
          </Link>
        ))}
      </nav>

      <div className="space-y-2 border-t border-surface-container-high pt-4">
        {canAccessDonorDashboard && (
          <Link
            to="/donations/external/new"
            className="flex w-full items-center justify-center gap-2 rounded-xl bg-primary py-3 font-bold text-white shadow-lg shadow-primary/20"
          >
            <span className="material-symbols-outlined">add</span>
            Doação Externa
          </Link>
        )}

        <button
          type="button"
          onClick={onLogout}
          className="flex w-full items-center gap-3 rounded-xl px-4 py-3 text-sm font-medium text-text-secondary hover:bg-[#f9f9fb] hover:text-primary"
        >
          <span className="material-symbols-outlined">logout</span>
          Sair
        </button>
      </div>
    </aside>
  );
}
