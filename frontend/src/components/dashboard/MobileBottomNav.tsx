import { Link, useLocation } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { hasDonorRole, hasRequesterRole, hasAdminRole } from "../../routes/roleRouting";

type MobileBottomNavProps = {
  activeItem?: "donor-dashboard" | "donations" | "requests" | "new-request" | "external-donation" | "profile";
};

type MenuItem = {
  key: string;
  icon: string;
  label: string;
  path: string;
};

export function MobileBottomNav({ activeItem }: MobileBottomNavProps) {
  const location = useLocation();
  const { roles } = useAuth();
  const canAccessRequesterArea = hasRequesterRole(roles);
  const canAccessDonorDashboard = hasDonorRole(roles);
  const canAccessAdminArea = hasAdminRole(roles);
  const showDonorDashboard = canAccessDonorDashboard || canAccessAdminArea;

  const menuItems: MenuItem[] = [
    ...(showDonorDashboard
      ? [
          { key: "donor-dashboard", icon: "home_health", label: "Central", path: "/dashboard" },
          { key: "donations", icon: "water_drop", label: "Doações", path: "/donations" },
        ]
      : []),
    ...(canAccessRequesterArea || canAccessAdminArea
      ? [
          { key: "requests", icon: "assignment", label: "Requisições", path: "/requests" },
          { key: "new-request", icon: "add_circle", label: "Nova", path: "/requests/new" },
        ]
      : []),
    { key: "profile", icon: "person", label: "Perfil", path: "/profile" },
  ];

  return (
    <nav
      className="lg:hidden fixed bottom-0 left-0 right-0 z-40 bg-white/95 backdrop-blur-xl border-t border-surface-container-high px-2 py-1.5 shadow-2xl flex items-center justify-around"
      aria-label="Navegação inferior mobile"
    >
      {menuItems.map((item) => {
        const isActive = activeItem === item.key || location.pathname === item.path;
        return (
          <Link
            key={item.key}
            to={item.path}
            className={`flex flex-col items-center justify-center flex-1 py-1 px-1 rounded-xl transition-all duration-200 text-center ${
              isActive
                ? "text-primary font-bold bg-red-50/80 scale-105"
                : "text-secondary hover:text-primary hover:bg-gray-50 font-medium"
            }`}
          >
            <span className={`material-symbols-outlined text-xl ${isActive ? "text-primary fill-1" : ""}`}>
              {item.icon}
            </span>
            <span className="text-[10px] mt-0.5 tracking-tight truncate max-w-full">{item.label}</span>
          </Link>
        );
      })}
    </nav>
  );
}
