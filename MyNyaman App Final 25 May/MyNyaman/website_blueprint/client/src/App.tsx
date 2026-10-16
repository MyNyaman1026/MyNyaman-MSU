import { Toaster } from "@/components/ui/sonner";
import { TooltipProvider } from "@/components/ui/tooltip";
import NotFound from "@/pages/NotFound";
import { Route, Switch } from "wouter";
import ErrorBoundary from "./components/ErrorBoundary";
import { ThemeProvider } from "./contexts/ThemeContext";
import { FirebaseAuthProvider } from "./contexts/FirebaseAuthContext";
import { ChildProvider } from "./contexts/ChildContext";
import Home from "./pages/Home";
import LandingPage from "./pages/LandingPage";
import SignIn from "./pages/SignIn";
import Register from "./pages/Register";
import VerifyEmail from "./pages/VerifyEmail";
import ParentDashboard from "./pages/ParentDashboard";
import ChildDashboard from "./pages/ChildDashboard";
import DailyLimits from "./pages/DailyLimits";
import UsageReports from "./pages/UsageReports";
import Chat from "./pages/Chat";
import AppBlocking from "./pages/AppBlocking";
import Profile from "./pages/Profile";
import Settings from "./pages/Settings";
import Menu from "./pages/Menu";
import AddChild from "./pages/AddChild";
import ConsultantDashboard from "./pages/ConsultantDashboard";
import NotificationCenter from "./pages/NotificationCenter";
import ManageChildren from "./pages/ManageChildren";
import ChildDashboardView from "./pages/ChildDashboardView";
import ConsultantView from "./pages/ConsultantView";
import ChildSignIn from "./pages/ChildSignIn";
import ChildDashboardOwn from "./pages/ChildDashboardOwn";
import ChildRegister from "./pages/ChildRegister";
import ChildDashboardProper from "./pages/ChildDashboardProper";
import { RoleProvider } from "./contexts/RoleContext";

function Router() {
  return (
    <Switch>
      <Route path={"/"} component={LandingPage} />
      <Route path={"/signin"} component={SignIn} />
      <Route path={"/register"} component={Register} />
      <Route path={"/verify-email"} component={VerifyEmail} />
      <Route path={"/home"} component={Home} />
      <Route path={"/dashboard/parent"} component={ParentDashboard} />
      <Route path={"/dashboard/child"} component={ChildDashboard} />
      <Route path={"/dashboard/consultant"} component={ConsultantDashboard} />
      <Route path={"/notifications"} component={NotificationCenter} />
      <Route path={"/daily-limits"} component={DailyLimits} />
      <Route path={"/usage-reports"} component={UsageReports} />
      <Route path={"/chat"} component={Chat} />
      <Route path={"/app-blocking"} component={AppBlocking} />
      <Route path={"/profile"} component={Profile} />
      <Route path={"/settings"} component={Settings} />
      <Route path={"/menu"} component={Menu} />
      <Route path={"/add-child"} component={AddChild} />
      <Route path={"/manage-children"} component={ManageChildren} />
      <Route path={"/dashboard/child-view"} component={ChildDashboardView} />
      <Route path={"/consultant-view"} component={ConsultantView} />
      <Route path={"/child/signin"} component={ChildSignIn} />
      <Route path={"/child/register"} component={ChildRegister} />
      <Route path={"/dashboard/child-own"} component={ChildDashboardOwn} />
      <Route path={"/child/dashboard"} component={ChildDashboardProper} />
      <Route path={"/:rest*"} component={NotFound} />
      <Route path={"/404"} component={NotFound} />
      {/* Final fallback route */}
      <Route component={NotFound} />
    </Switch>
  );
}

// NOTE: About Theme
// - First choose a default theme according to your design style (dark or light bg), than change color palette in index.css
//   to keep consistent foreground/background color across components
// - If you want to make theme switchable, pass `switchable` ThemeProvider and use `useTheme` hook

function App() {
  return (
    <ErrorBoundary>
      <FirebaseAuthProvider>
        <RoleProvider>
          <ChildProvider>
            <ThemeProvider defaultTheme="light">
            <TooltipProvider>
              <Toaster />
              <Router />
            </TooltipProvider>
            </ThemeProvider>
          </ChildProvider>
        </RoleProvider>
      </FirebaseAuthProvider>
    </ErrorBoundary>
  );
}

export default App;
